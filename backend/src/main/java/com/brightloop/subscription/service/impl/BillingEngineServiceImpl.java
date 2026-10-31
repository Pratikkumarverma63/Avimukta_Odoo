package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.*;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.service.BillingEngineService;
import com.brightloop.subscription.service.DiscountService;
import com.brightloop.subscription.service.PricingCalculationService;
import com.brightloop.subscription.util.BillingDateCalculator;
import com.brightloop.subscription.web.dto.BillingExecutionSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BillingEngineServiceImpl implements BillingEngineService {

    private static final Logger log = LoggerFactory.getLogger(BillingEngineServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final NotificationRepository notificationRepository;
    private final PricingCalculationService pricingCalculationService;
    private final DiscountService discountService;
    private final TransactionTemplate transactionTemplate;
    private final ReentrantLock billingJobLock = new ReentrantLock();

    @Value("${app.billing.enabled:true}")
    private boolean billingEnabled;

    public BillingEngineServiceImpl(
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            BillingRecordRepository billingRecordRepository,
            NotificationRepository notificationRepository,
            PricingCalculationService pricingCalculationService,
            DiscountService discountService,
            PlatformTransactionManager transactionManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.notificationRepository = notificationRepository;
        this.pricingCalculationService = pricingCalculationService;
        this.discountService = discountService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Daily scheduled cron execution.
     */
    @Scheduled(cron = "${app.billing.cron:0 0 1 * * ?}")
    public void scheduledBillingJob() {
        if (!billingEnabled) {
            log.info("Scheduled billing job is disabled by configuration.");
            return;
        }
        log.info("Starting automated daily recurring billing engine...");
        runBillingJob(LocalDate.now());
    }

    @Override
    public BillingExecutionSummaryResponse runBillingJob(LocalDate asOfDate) {
        if (!billingJobLock.tryLock()) {
            log.warn("Billing job execution already running in another thread. Concurrency lock held.");
            return BillingExecutionSummaryResponse.builder()
                    .executionDate(asOfDate)
                    .subscriptionsProcessed(0)
                    .skippedCount(0)
                    .failedCount(0)
                    .totalBilledAmount(BigDecimal.ZERO)
                    .entries(List.of(BillingExecutionSummaryResponse.BillingLogEntry.builder()
                            .status("SKIPPED")
                            .message("Concurrent billing job already in progress")
                            .build()))
                    .build();
        }

        try {
            LocalDate targetDate = asOfDate != null ? asOfDate : LocalDate.now();
            List<Subscription> dueSubscriptions = subscriptionRepository.findSubscriptionsDueForBilling(
                    SubscriptionStatus.ACTIVE, targetDate);

            log.info("Found {} active subscriptions due for billing on {}", dueSubscriptions.size(), targetDate);

            int processed = 0;
            int generated = 0;
            int skipped = 0;
            int failed = 0;
            BigDecimal totalBilled = BigDecimal.ZERO;
            List<BillingExecutionSummaryResponse.BillingLogEntry> entries = new ArrayList<>();

            for (Subscription dueSub : dueSubscriptions) {
                processed++;
                Long subId = dueSub.getId();
                String subNumber = dueSub.getSubscriptionNumber();
                String customerName = dueSub.getCustomer() != null ? dueSub.getCustomer().getCompanyName() : "Customer";

                try {
                    BillingResult result = transactionTemplate.execute(status -> processSubscriptionBilling(subId, targetDate));
                    if (result != null) {
                        if ("SUCCESS".equals(result.status)) {
                            generated++;
                            totalBilled = totalBilled.add(result.billedAmount);
                        } else if ("SKIPPED".equals(result.status)) {
                            skipped++;
                        }
                        entries.add(BillingExecutionSummaryResponse.BillingLogEntry.builder()
                                .subscriptionId(subId)
                                .subscriptionNumber(subNumber)
                                .customerName(customerName)
                                .invoiceNumber(result.invoiceNumber)
                                .billedAmount(result.billedAmount)
                                .status(result.status)
                                .message(result.message)
                                .build());
                    }
                } catch (Exception ex) {
                    failed++;
                    log.error("Failed to bill subscription {}: {}", subNumber, ex.getMessage(), ex);
                    recordBillingFailure(subId, targetDate, ex.getMessage());
                    entries.add(BillingExecutionSummaryResponse.BillingLogEntry.builder()
                            .subscriptionId(subId)
                            .subscriptionNumber(subNumber)
                            .customerName(customerName)
                            .status("FAILED")
                            .message("Billing failed: " + ex.getMessage())
                            .build());
                }
            }

            return BillingExecutionSummaryResponse.builder()
                    .executionDate(targetDate)
                    .subscriptionsProcessed(processed)
                    .invoicesGenerated(generated)
                    .skippedCount(skipped)
                    .failedCount(failed)
                    .totalBilledAmount(totalBilled)
                    .entries(entries)
                    .build();

        } finally {
            billingJobLock.unlock();
        }
    }

    private record BillingResult(String status, String invoiceNumber, BigDecimal billedAmount, String message) {}

    private BillingResult processSubscriptionBilling(Long subId, LocalDate targetDate) {
        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found: " + subId));

        LocalDate periodStart = sub.getLastBilledDate() != null ?
                sub.getLastBilledDate().plusDays(1) : sub.getStartDate();
        LocalDate periodEnd = sub.getNextBillingDate() != null ?
                sub.getNextBillingDate() : targetDate;

        // Idempotency check: Guarantee no duplicate invoices for this exact subscription and billing period!
        if (billingRecordRepository.existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
                sub.getId(), periodStart, periodEnd)) {
            log.info("Subscription {} has already been billed for period {} to {}. Skipping duplicate billing.",
                    sub.getSubscriptionNumber(), periodStart, periodEnd);
            return new BillingResult("SKIPPED", null, BigDecimal.ZERO, "Already billed for period " + periodStart + " to " + periodEnd);
        }

        // Calculate Due Date based on payment terms
        int dueDays = 30;
        if ("IMMEDIATE".equalsIgnoreCase(sub.getPaymentTerms())) {
            dueDays = 0;
        } else if ("NET_15".equalsIgnoreCase(sub.getPaymentTerms())) {
            dueDays = 15;
        } else if ("NET_60".equalsIgnoreCase(sub.getPaymentTerms())) {
            dueDays = 60;
        }
        LocalDate dueDate = targetDate.plusDays(dueDays);

        String invoiceNumber = "INV-" + targetDate.getYear() + "-" + String.format("%05d", (int) (Math.random() * 90000) + 10000);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .subscription(sub)
                .customer(sub.getCustomer())
                .invoiceDate(targetDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.CONFIRMED)
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .notes("Automated recurring billing for period " + periodStart + " to " + periodEnd)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SubscriptionItem sItem : sub.getItems()) {
            PricingCalculationService.LineCalculationResult calc = pricingCalculationService.calculateLineItem(
                    sItem.getProduct(), sItem.getVariant(), sItem.getQuantity(), sItem.getUnitPrice(),
                    sItem.getTaxRule(), sItem.getDiscountRule(), targetDate);

            InvoiceItem iItem = InvoiceItem.builder()
                    .product(sItem.getProduct())
                    .variant(sItem.getVariant())
                    .description(sItem.getDescription())
                    .quantity(sItem.getQuantity())
                    .unitPrice(calc.unitPrice())
                    .taxRule(sItem.getTaxRule())
                    .taxAmount(calc.taxAmount())
                    .discountRule(sItem.getDiscountRule())
                    .discountAmount(calc.discountAmount())
                    .lineTotal(calc.lineTotal())
                    .build();

            invoice.addItem(iItem);

            subtotal = subtotal.add(calc.lineSubtotal());
            totalTax = totalTax.add(calc.taxAmount());
            totalDiscount = totalDiscount.add(calc.discountAmount());
            totalAmount = totalAmount.add(calc.lineTotal());
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(totalTax);
        invoice.setDiscountAmount(totalDiscount);
        invoice.setTotalAmount(totalAmount);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setOutstandingAmount(totalAmount);

        invoice = invoiceRepository.save(invoice);

        // Record discount usage if discounts were applied
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getDiscountRule() != null && item.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discountService.consumeDiscountUsage(
                        item.getDiscountRule().getId(), invoice.getId(), sub.getCustomer().getId(), item.getDiscountAmount());
            }
        }

        // Record Billing Record (Enforces uniqueness and audit log)
        BillingRecord billingRecord = BillingRecord.builder()
                .subscription(sub)
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .executionDate(targetDate)
                .status(BillingJobStatus.SUCCESS)
                .generatedInvoice(invoice)
                .build();
        billingRecordRepository.save(billingRecord);

        // Advance subscription billing state
        sub.setLastBilledDate(periodEnd);
        LocalDate nextDate = BillingDateCalculator.calculateNextBillingDate(periodEnd, sub.getPlan().getBillingFrequency());
        sub.setNextBillingDate(nextDate);

        // Expiry check
        if (sub.isAutoCloseOnExpiry() && sub.getExpiryDate() != null && nextDate.isAfter(sub.getExpiryDate())) {
            sub.setStatus(SubscriptionStatus.CLOSED);
            log.info("Subscription {} has reached its expiry date {}. Marked as CLOSED.", sub.getSubscriptionNumber(), sub.getExpiryDate());
        }

        subscriptionRepository.save(sub);

        // Create Customer In-App Notification
        Notification notification = Notification.builder()
                .customer(sub.getCustomer())
                .title("New Invoice Generated")
                .message("Invoice " + invoiceNumber + " for amount ₹" + totalAmount + " has been generated for your subscription " + sub.getSubscriptionNumber() + ".")
                .notificationType(NotificationType.INVOICE_GENERATED)
                .referenceType("INVOICE")
                .referenceId(invoice.getId())
                .build();
        notificationRepository.save(notification);

        log.info("Successfully generated invoice {} for subscription {}", invoiceNumber, sub.getSubscriptionNumber());

        return new BillingResult("SUCCESS", invoiceNumber, totalAmount, "Billed successfully");
    }

    private void recordBillingFailure(Long subId, LocalDate targetDate, String errorMsg) {
        transactionTemplate.execute(status -> {
            Subscription sub = subscriptionRepository.findById(subId).orElse(null);
            if (sub == null) return null;

            LocalDate periodStart = sub.getLastBilledDate() != null ?
                    sub.getLastBilledDate().plusDays(1) : sub.getStartDate();
            LocalDate periodEnd = sub.getNextBillingDate() != null ?
                    sub.getNextBillingDate() : targetDate;

            BillingRecord failRecord = BillingRecord.builder()
                    .subscription(sub)
                    .billingPeriodStart(periodStart)
                    .billingPeriodEnd(periodEnd)
                    .executionDate(targetDate)
                    .status(BillingJobStatus.FAILED)
                    .errorMessage(errorMsg)
                    .build();
            return billingRecordRepository.save(failRecord);
        });
    }
}

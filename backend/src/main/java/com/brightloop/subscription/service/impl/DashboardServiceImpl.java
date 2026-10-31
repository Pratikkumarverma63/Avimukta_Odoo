package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.BillingFrequency;
import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.service.DashboardService;
import com.brightloop.subscription.web.dto.DashboardMetricsResponse;
import com.brightloop.subscription.web.dto.RevenueTrendResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public DashboardServiceImpl(
            CustomerRepository customerRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public DashboardMetricsResponse getMetrics(LocalDate startDate, LocalDate endDate, Long customerId) {
        long totalCustomers = customerId != null ? 1 : customerRepository.count();
        long totalSubs = customerId != null ?
                subscriptionRepository.findByCustomerId(customerId, Pageable.unpaged()).getTotalElements() :
                subscriptionRepository.count();

        List<Subscription> activeSubs = customerId != null ?
                subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE, Pageable.unpaged()).getContent() :
                subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE, Pageable.unpaged()).getContent();

        long activeSubsCount = activeSubs.size();

        // Calculate Monthly Recurring Revenue (MRR)
        BigDecimal mrr = BigDecimal.ZERO;
        for (Subscription sub : activeSubs) {
            BigDecimal subMonthly = calculateMonthlyValue(sub);
            mrr = mrr.add(subMonthly);
        }
        mrr = mrr.setScale(2, RoundingMode.HALF_UP);
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInvoiced = invoiceRepository.calculateTotalInvoicedRevenue();
        BigDecimal totalPayments = customerId != null ?
                paymentRepository.calculateTotalPaymentsReceivedForCustomer(customerId) :
                paymentRepository.calculateTotalPaymentsReceived();
        BigDecimal totalOutstanding = customerId != null ?
                invoiceRepository.calculateTotalOutstandingForCustomer(customerId) :
                invoiceRepository.calculateTotalOutstanding();

        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = customerId != null ?
                invoiceRepository.findOverdueInvoicesForCustomer(customerId, today) :
                invoiceRepository.findOverdueInvoices(today);

        long overdueCount = overdueInvoices.size();
        BigDecimal overdueAmount = overdueInvoices.stream()
                .map(Invoice::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Upcoming renewals in next 30 days
        List<Subscription> upcomingRenewals = subscriptionRepository.findUpcomingRenewals(today, today.plusDays(30));
        long upcomingRenewalsCount = upcomingRenewals.size();

        // Breakdowns
        Map<String, Long> subStatusMap = new HashMap<>();
        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            long count = subscriptionRepository.countByStatus(status);
            subStatusMap.put(status.name(), count);
        }

        Map<String, Long> invStatusMap = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            long count = invoiceRepository.findByStatus(status, Pageable.unpaged()).getTotalElements();
            invStatusMap.put(status.name(), count);
        }

        Map<String, BigDecimal> paymentsByMethod = new HashMap<>();
        List<Payment> allPayments = paymentRepository.findAll();
        for (Payment p : allPayments) {
            String method = p.getPaymentMethod().name();
            paymentsByMethod.put(method, paymentsByMethod.getOrDefault(method, BigDecimal.ZERO).add(p.getAmount()));
        }

        return DashboardMetricsResponse.builder()
                .totalCustomers(totalCustomers)
                .activeSubscriptions(activeSubsCount)
                .totalSubscriptions(totalSubs)
                .monthlyRecurringRevenue(mrr)
                .annualRunRate(arr)
                .totalInvoicedRevenue(totalInvoiced)
                .totalPaymentsReceived(totalPayments)
                .totalOutstandingAmount(totalOutstanding)
                .overdueInvoicesCount(overdueCount)
                .overdueInvoicesAmount(overdueAmount)
                .upcomingRenewalsCount(upcomingRenewalsCount)
                .subscriptionsByStatus(subStatusMap)
                .invoicesByStatus(invStatusMap)
                .paymentsByMethod(paymentsByMethod)
                .build();
    }

    @Override
    public RevenueTrendResponse getRevenueTrend() {
        YearMonth current = YearMonth.now();
        List<RevenueTrendResponse.MonthlyTrendPoint> points = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal invoiced = invoiceRepository.calculateInvoicedRevenueBetween(start, end);

            OffsetDateTime startDt = start.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime endDt = end.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset());
            BigDecimal collected = paymentRepository.calculatePaymentsReceivedBetween(startDt, endDt);

            BigDecimal outstanding = invoiced.subtract(collected).max(BigDecimal.ZERO);
            long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

            points.add(RevenueTrendResponse.MonthlyTrendPoint.builder()
                    .month(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .invoicedAmount(invoiced)
                    .collectedAmount(collected)
                    .outstandingAmount(outstanding)
                    .activeSubscriptionsCount(activeSubs)
                    .build());
        }

        return RevenueTrendResponse.builder()
                .monthlyPoints(points)
                .build();
    }

    private BigDecimal calculateMonthlyValue(Subscription sub) {
        BigDecimal total = sub.getTotalAmount();
        BillingFrequency freq = sub.getPlan().getBillingFrequency();

        return switch (freq) {
            case DAILY -> total.multiply(BigDecimal.valueOf(30));
            case WEEKLY -> total.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case MONTHLY -> total;
            case YEARLY -> total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }
}

package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.PricingCalculationService;
import com.brightloop.subscription.service.SubscriptionService;
import com.brightloop.subscription.util.BillingDateCalculator;
import com.brightloop.subscription.web.dto.*;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ForbiddenException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final DiscountRuleRepository discountRuleRepository;
    private final QuotationRepository quotationRepository;
    private final PricingCalculationService pricingCalculationService;
    private final AuditLogService auditLogService;

    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            CustomerRepository customerRepository,
            PlanRepository planRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            TaxRuleRepository taxRuleRepository,
            DiscountRuleRepository discountRuleRepository,
            QuotationRepository quotationRepository,
            PricingCalculationService pricingCalculationService,
            AuditLogService auditLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.discountRuleRepository = discountRuleRepository;
        this.quotationRepository = quotationRepository;
        this.pricingCalculationService = pricingCalculationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.getPlanId()));

        Quotation quotation = null;
        if (request.getQuotationId() != null) {
            quotation = quotationRepository.findById(request.getQuotationId()).orElse(null);
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate expiryDate = request.getExpiryDate();
        if (expiryDate == null) {
            expiryDate = startDate.plusYears(1);
        }

        LocalDate nextBillingDate = BillingDateCalculator.calculateNextBillingDate(startDate, plan.getBillingFrequency());
        String subNumber = generateSubscriptionNumber();

        Subscription subscription = Subscription.builder()
                .subscriptionNumber(subNumber)
                .customer(customer)
                .plan(plan)
                .quotation(quotation)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .nextBillingDate(nextBillingDate)
                .paymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : "NET_30")
                .autoCloseOnExpiry(request.isAutoCloseOnExpiry())
                .allowPause(plan.isAllowPause() && request.isAllowPause())
                .allowEarlyClosure(plan.isAllowEarlyClosure() && request.isAllowEarlyClosure())
                .allowRenewal(plan.isAllowRenewal() && request.isAllowRenewal())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SubscriptionItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            ProductVariant variant = null;
            if (itemReq.getVariantId() != null) {
                variant = variantRepository.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + itemReq.getVariantId()));
            }

            TaxRule taxRule = null;
            if (itemReq.getTaxRuleId() != null) {
                taxRule = taxRuleRepository.findById(itemReq.getTaxRuleId()).orElse(null);
            }

            DiscountRule discountRule = null;
            if (itemReq.getDiscountRuleId() != null) {
                discountRule = discountRuleRepository.findById(itemReq.getDiscountRuleId()).orElse(null);
            }

            PricingCalculationService.LineCalculationResult calc = pricingCalculationService.calculateLineItem(
                    product, variant, itemReq.getQuantity(), itemReq.getUnitPrice(),
                    taxRule, discountRule, startDate);

            String desc = itemReq.getDescription();
            if (desc == null || desc.isBlank()) {
                desc = product.getName() + (variant != null ? " (" + variant.getVariantValue() + ")" : "");
            }

            SubscriptionItem item = SubscriptionItem.builder()
                    .product(product)
                    .variant(variant)
                    .description(desc)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(calc.unitPrice())
                    .taxRule(taxRule)
                    .taxAmount(calc.taxAmount())
                    .discountRule(discountRule)
                    .discountAmount(calc.discountAmount())
                    .lineTotal(calc.lineTotal())
                    .build();

            subscription.addItem(item);

            subtotal = subtotal.add(calc.lineSubtotal());
            totalTax = totalTax.add(calc.taxAmount());
            totalDiscount = totalDiscount.add(calc.discountAmount());
            totalAmount = totalAmount.add(calc.lineTotal());
        }

        subscription.setSubtotal(subtotal);
        subscription.setTaxAmount(totalTax);
        subscription.setDiscountAmount(totalDiscount);
        subscription.setTotalAmount(totalAmount);

        subscription = subscriptionRepository.save(subscription);
        auditLogService.logAction("SUBSCRIPTION_CREATED", "Subscription", subscription.getId(), null, subscription.getSubscriptionNumber());

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        verifyCustomerAccess(subscription.getCustomer().getId());

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getAllSubscriptions(Pageable pageable, SubscriptionStatus status, Long customerId) {
        Long isolatedCustomerId = resolveCustomerIdForQuery(customerId);

        Page<Subscription> page;
        if (isolatedCustomerId != null && status != null) {
            page = subscriptionRepository.findByCustomerIdAndStatus(isolatedCustomerId, status, pageable);
        } else if (isolatedCustomerId != null) {
            page = subscriptionRepository.findByCustomerId(isolatedCustomerId, pageable);
        } else if (status != null) {
            page = subscriptionRepository.findByStatus(status, pageable);
        } else {
            page = subscriptionRepository.findAll(pageable);
        }

        return page.map(this::mapToSubscriptionResponse);
    }

    @Override
    public SubscriptionResponse pauseSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        verifyCustomerAccess(subscription.getCustomer().getId());

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE subscriptions can be paused. Current status is " + subscription.getStatus());
        }

        if (!subscription.isAllowPause()) {
            throw new BadRequestException("This subscription's plan does not permit pausing.");
        }

        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription = subscriptionRepository.save(subscription);
        auditLogService.logAction("SUBSCRIPTION_PAUSED", "Subscription", subscription.getId(), "ACTIVE", "PAUSED");

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse resumeSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        verifyCustomerAccess(subscription.getCustomer().getId());

        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new BadRequestException("Only PAUSED subscriptions can be resumed. Current status is " + subscription.getStatus());
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // If next billing date passed during pause, set next billing date to next cycle from today
        LocalDate today = LocalDate.now();
        if (subscription.getNextBillingDate() == null || subscription.getNextBillingDate().isBefore(today)) {
            LocalDate nextDate = BillingDateCalculator.calculateNextBillingDate(today, subscription.getPlan().getBillingFrequency());
            subscription.setNextBillingDate(nextDate);
        }

        subscription = subscriptionRepository.save(subscription);
        auditLogService.logAction("SUBSCRIPTION_RESUMED", "Subscription", subscription.getId(), "PAUSED", "ACTIVE");

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse closeSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        verifyCustomerAccess(subscription.getCustomer().getId());

        if (subscription.getStatus() == SubscriptionStatus.CLOSED) {
            throw new BadRequestException("Subscription is already closed.");
        }

        if (!subscription.isAllowEarlyClosure() && (subscription.getExpiryDate() == null || subscription.getExpiryDate().isAfter(LocalDate.now()))) {
            throw new BadRequestException("This subscription's plan does not permit early closure.");
        }

        SubscriptionStatus oldStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.CLOSED);
        subscription = subscriptionRepository.save(subscription);
        auditLogService.logAction("SUBSCRIPTION_CLOSED", "Subscription", subscription.getId(), oldStatus.name(), "CLOSED");

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse renewSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        verifyCustomerAccess(subscription.getCustomer().getId());

        if (!subscription.isAllowRenewal()) {
            throw new BadRequestException("This subscription's plan does not permit renewal.");
        }

        LocalDate currentExpiry = subscription.getExpiryDate() != null ? subscription.getExpiryDate() : LocalDate.now();
        LocalDate newExpiry = currentExpiry.plusYears(1);
        subscription.setExpiryDate(newExpiry);

        if (subscription.getStatus() == SubscriptionStatus.CLOSED) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setNextBillingDate(BillingDateCalculator.calculateNextBillingDate(LocalDate.now(), subscription.getPlan().getBillingFrequency()));
        }

        subscription = subscriptionRepository.save(subscription);
        auditLogService.logAction("SUBSCRIPTION_RENEWED", "Subscription", subscription.getId(), currentExpiry.toString(), newExpiry.toString());

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    public void autoCloseExpiredSubscriptions() {
        List<Subscription> expiredList = subscriptionRepository.findExpiredSubscriptionsForAutoClose(LocalDate.now());
        for (Subscription sub : expiredList) {
            sub.setStatus(SubscriptionStatus.CLOSED);
            subscriptionRepository.save(sub);
            auditLogService.logAction("AUTO_CLOSE_ON_EXPIRY", "Subscription", sub.getId(), "ACTIVE", "CLOSED");
        }
    }

    private void verifyCustomerAccess(Long targetCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            Long currentCustId = SecurityUtils.getCurrentCustomerId();
            if (currentCustId == null || !currentCustId.equals(targetCustomerId)) {
                throw new ForbiddenException("Access Denied: You do not have permission to access another customer's data.");
            }
        }
    }

    private Long resolveCustomerIdForQuery(Long requestedCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            Long currentCustId = SecurityUtils.getCurrentCustomerId();
            if (currentCustId != null) {
                return currentCustId;
            }
            if (requestedCustomerId != null) {
                return requestedCustomerId;
            }
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                return customerRepository.findByUserId(userId).map(Customer::getId).orElse(null);
            }
        }
        return requestedCustomerId;
    }

    private String generateSubscriptionNumber() {
        return "SUB-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 90000) + 10000);
    }

    public SubscriptionResponse mapToSubscriptionResponse(Subscription s) {
        List<SubscriptionItemResponse> itemResponses = s.getItems().stream()
                .map(i -> SubscriptionItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productCode(i.getProduct().getCode())
                        .productName(i.getProduct().getName())
                        .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                        .variantName(i.getVariant() != null ? i.getVariant().getAttributeName() + ": " + i.getVariant().getVariantValue() : null)
                        .description(i.getDescription())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .taxRuleId(i.getTaxRule() != null ? i.getTaxRule().getId() : null)
                        .taxRuleName(i.getTaxRule() != null ? i.getTaxRule().getName() : null)
                        .taxAmount(i.getTaxAmount())
                        .discountRuleId(i.getDiscountRule() != null ? i.getDiscountRule().getId() : null)
                        .discountRuleName(i.getDiscountRule() != null ? i.getDiscountRule().getName() : null)
                        .discountAmount(i.getDiscountAmount())
                        .lineTotal(i.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return SubscriptionResponse.builder()
                .id(s.getId())
                .subscriptionNumber(s.getSubscriptionNumber())
                .customerId(s.getCustomer().getId())
                .customerCode(s.getCustomer().getCustomerCode())
                .customerCompanyName(s.getCustomer().getCompanyName())
                .customerEmail(s.getCustomer().getEmail())
                .planId(s.getPlan().getId())
                .planName(s.getPlan().getName())
                .planCode(s.getPlan().getCode())
                .billingFrequency(s.getPlan().getBillingFrequency())
                .quotationId(s.getQuotation() != null ? s.getQuotation().getId() : null)
                .quotationNumber(s.getQuotation() != null ? s.getQuotation().getQuotationNumber() : null)
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .expiryDate(s.getExpiryDate())
                .nextBillingDate(s.getNextBillingDate())
                .lastBilledDate(s.getLastBilledDate())
                .paymentTerms(s.getPaymentTerms())
                .autoCloseOnExpiry(s.isAutoCloseOnExpiry())
                .allowPause(s.isAllowPause())
                .allowEarlyClosure(s.isAllowEarlyClosure())
                .allowRenewal(s.isAllowRenewal())
                .subtotal(s.getSubtotal())
                .taxAmount(s.getTaxAmount())
                .discountAmount(s.getDiscountAmount())
                .totalAmount(s.getTotalAmount())
                .items(itemResponses)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

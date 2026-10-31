package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.QuotationStatus;
import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.PricingCalculationService;
import com.brightloop.subscription.service.QuotationService;
import com.brightloop.subscription.service.SubscriptionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationTemplateRepository templateRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final DiscountRuleRepository discountRuleRepository;
    private final SubscriptionService subscriptionService;
    private final PricingCalculationService pricingCalculationService;
    private final AuditLogService auditLogService;

    public QuotationServiceImpl(
            QuotationRepository quotationRepository,
            QuotationTemplateRepository templateRepository,
            CustomerRepository customerRepository,
            PlanRepository planRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            TaxRuleRepository taxRuleRepository,
            DiscountRuleRepository discountRuleRepository,
            SubscriptionService subscriptionService,
            PricingCalculationService pricingCalculationService,
            AuditLogService auditLogService) {
        this.quotationRepository = quotationRepository;
        this.templateRepository = templateRepository;
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.discountRuleRepository = discountRuleRepository;
        this.subscriptionService = subscriptionService;
        this.pricingCalculationService = pricingCalculationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public QuotationResponse createQuotation(QuotationRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.getPlanId()));

        LocalDate issueDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now();
        LocalDate expiryDate = request.getExpiryDate() != null ? request.getExpiryDate() : issueDate.plusDays(30);

        User currentUser = SecurityUtils.getCurrentUser().orElse(null);
        String quoteNumber = generateQuotationNumber();

        Quotation quotation = Quotation.builder()
                .quotationNumber(quoteNumber)
                .customer(customer)
                .plan(plan)
                .status(QuotationStatus.DRAFT)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .paymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : "NET_30")
                .notes(request.getNotes())
                .createdBy(currentUser)
                .build();

        populateQuotationItems(quotation, request.getItems(), issueDate);

        quotation = quotationRepository.save(quotation);
        auditLogService.logAction("QUOTATION_CREATED", "Quotation", quotation.getId(), null, quotation.getQuotationNumber());

        return mapToQuotationResponse(quotation);
    }

    @Override
    public QuotationResponse createFromTemplate(Long templateId, Long customerId, String paymentTerms) {
        QuotationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + templateId));

        if (template.getDefaultPlan() == null) {
            throw new BadRequestException("Template does not specify a default plan.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        LocalDate issueDate = LocalDate.now();
        LocalDate expiryDate = issueDate.plusDays(template.getValidityDays() != null ? template.getValidityDays() : 30);
        User currentUser = SecurityUtils.getCurrentUser().orElse(null);

        Quotation quotation = Quotation.builder()
                .quotationNumber(generateQuotationNumber())
                .customer(customer)
                .plan(template.getDefaultPlan())
                .status(QuotationStatus.DRAFT)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .paymentTerms(paymentTerms != null ? paymentTerms : "NET_30")
                .notes("Generated from template: " + template.getName())
                .createdBy(currentUser)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (QuotationTemplateItem tItem : template.getItems()) {
            PricingCalculationService.LineCalculationResult calc = pricingCalculationService.calculateLineItem(
                    tItem.getProduct(), tItem.getVariant(), tItem.getDefaultQuantity(), tItem.getUnitPrice(),
                    null, null, issueDate);

            String desc = tItem.getProduct().getName() +
                    (tItem.getVariant() != null ? " (" + tItem.getVariant().getVariantValue() + ")" : "");

            QuotationItem qItem = QuotationItem.builder()
                    .product(tItem.getProduct())
                    .variant(tItem.getVariant())
                    .description(desc)
                    .quantity(tItem.getDefaultQuantity())
                    .unitPrice(calc.unitPrice())
                    .taxAmount(calc.taxAmount())
                    .discountAmount(calc.discountAmount())
                    .lineTotal(calc.lineTotal())
                    .build();

            quotation.addItem(qItem);

            subtotal = subtotal.add(calc.lineSubtotal());
            totalTax = totalTax.add(calc.taxAmount());
            totalDiscount = totalDiscount.add(calc.discountAmount());
            totalAmount = totalAmount.add(calc.lineTotal());
        }

        quotation.setSubtotal(subtotal);
        quotation.setTaxAmount(totalTax);
        quotation.setDiscountAmount(totalDiscount);
        quotation.setTotalAmount(totalAmount);

        quotation = quotationRepository.save(quotation);
        auditLogService.logAction("QUOTATION_FROM_TEMPLATE", "Quotation", quotation.getId(), null, quotation.getQuotationNumber());

        return mapToQuotationResponse(quotation);
    }

    @Override
    public QuotationResponse updateQuotation(Long id, QuotationRequest request) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT quotations can be modified. Current status: " + quotation.getStatus());
        }

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.getPlanId()));

        quotation.setPlan(plan);
        if (request.getIssueDate() != null) quotation.setIssueDate(request.getIssueDate());
        if (request.getExpiryDate() != null) quotation.setExpiryDate(request.getExpiryDate());
        if (request.getPaymentTerms() != null) quotation.setPaymentTerms(request.getPaymentTerms());
        if (request.getNotes() != null) quotation.setNotes(request.getNotes());

        quotation.getItems().clear();
        populateQuotationItems(quotation, request.getItems(), quotation.getIssueDate());

        quotation = quotationRepository.save(quotation);
        auditLogService.logAction("QUOTATION_UPDATED", "Quotation", quotation.getId(), null, quotation.getQuotationNumber());

        return mapToQuotationResponse(quotation);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationResponse getQuotationById(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));

        verifyCustomerAccess(quotation.getCustomer().getId());

        return mapToQuotationResponse(quotation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuotationResponse> getAllQuotations(Pageable pageable, QuotationStatus status, Long customerId) {
        Long isolatedCustomerId = resolveCustomerIdForQuery(customerId);

        Page<Quotation> page;
        if (isolatedCustomerId != null && status != null) {
            page = quotationRepository.findByCustomerIdAndStatus(isolatedCustomerId, status, pageable);
        } else if (isolatedCustomerId != null) {
            page = quotationRepository.findByCustomerId(isolatedCustomerId, pageable);
        } else if (status != null) {
            page = quotationRepository.findByStatus(status, pageable);
        } else {
            page = quotationRepository.findAll(pageable);
        }

        return page.map(this::mapToQuotationResponse);
    }

    @Override
    public QuotationResponse sendQuotation(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT quotations can be dispatched. Current status: " + quotation.getStatus());
        }

        quotation.setStatus(QuotationStatus.QUOTATION);
        quotation = quotationRepository.save(quotation);
        auditLogService.logAction("QUOTATION_SENT", "Quotation", quotation.getId(), "DRAFT", "QUOTATION");

        return mapToQuotationResponse(quotation);
    }

    @Override
    public SubscriptionResponse confirmQuotation(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));

        verifyCustomerAccess(quotation.getCustomer().getId());

        if (quotation.getStatus() == QuotationStatus.CONFIRMED) {
            throw new BadRequestException("Quotation is already confirmed.");
        }

        if (quotation.getStatus() == QuotationStatus.CANCELLED) {
            throw new BadRequestException("Cancelled quotation cannot be confirmed.");
        }

        if (quotation.getExpiryDate().isBefore(LocalDate.now())) {
            quotation.setStatus(QuotationStatus.EXPIRED);
            quotationRepository.save(quotation);
            throw new BadRequestException("Quotation has expired on " + quotation.getExpiryDate() + " and cannot be confirmed.");
        }

        quotation.setStatus(QuotationStatus.CONFIRMED);
        quotationRepository.save(quotation);

        // Convert to Active Subscription
        List<SubscriptionItemRequest> subItems = quotation.getItems().stream()
                .map(qi -> SubscriptionItemRequest.builder()
                        .productId(qi.getProduct().getId())
                        .variantId(qi.getVariant() != null ? qi.getVariant().getId() : null)
                        .description(qi.getDescription())
                        .quantity(qi.getQuantity())
                        .unitPrice(qi.getUnitPrice())
                        .taxRuleId(qi.getTaxRule() != null ? qi.getTaxRule().getId() : null)
                        .discountRuleId(qi.getDiscountRule() != null ? qi.getDiscountRule().getId() : null)
                        .build())
                .collect(Collectors.toList());

        SubscriptionRequest subRequest = SubscriptionRequest.builder()
                .customerId(quotation.getCustomer().getId())
                .planId(quotation.getPlan().getId())
                .quotationId(quotation.getId())
                .startDate(LocalDate.now())
                .paymentTerms(quotation.getPaymentTerms())
                .items(subItems)
                .build();

        SubscriptionResponse subscriptionResponse = subscriptionService.createSubscription(subRequest);
        auditLogService.logAction("QUOTATION_CONVERTED_TO_SUBSCRIPTION", "Quotation", quotation.getId(), quotation.getQuotationNumber(), subscriptionResponse.getSubscriptionNumber());

        return subscriptionResponse;
    }

    @Override
    public QuotationResponse cancelQuotation(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));

        verifyCustomerAccess(quotation.getCustomer().getId());

        if (quotation.getStatus() == QuotationStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel already confirmed quotation.");
        }

        QuotationStatus oldStatus = quotation.getStatus();
        quotation.setStatus(QuotationStatus.CANCELLED);
        quotation = quotationRepository.save(quotation);
        auditLogService.logAction("QUOTATION_CANCELLED", "Quotation", quotation.getId(), oldStatus.name(), "CANCELLED");

        return mapToQuotationResponse(quotation);
    }

    private void populateQuotationItems(Quotation quotation, List<QuotationItemRequest> itemRequests, LocalDate calculationDate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (QuotationItemRequest itemReq : itemRequests) {
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
                    taxRule, discountRule, calculationDate);

            String desc = itemReq.getDescription();
            if (desc == null || desc.isBlank()) {
                desc = product.getName() + (variant != null ? " (" + variant.getVariantValue() + ")" : "");
            }

            QuotationItem item = QuotationItem.builder()
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

            quotation.addItem(item);

            subtotal = subtotal.add(calc.lineSubtotal());
            totalTax = totalTax.add(calc.taxAmount());
            totalDiscount = totalDiscount.add(calc.discountAmount());
            totalAmount = totalAmount.add(calc.lineTotal());
        }

        quotation.setSubtotal(subtotal);
        quotation.setTaxAmount(totalTax);
        quotation.setDiscountAmount(totalDiscount);
        quotation.setTotalAmount(totalAmount);
    }

    private void verifyCustomerAccess(Long targetCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            Long currentCustId = SecurityUtils.getCurrentCustomerId();
            if (currentCustId == null || !currentCustId.equals(targetCustomerId)) {
                throw new ForbiddenException("Access Denied: You do not have permission to access another customer's quotation.");
            }
        }
    }

    private Long resolveCustomerIdForQuery(Long requestedCustomerId) {
        if (SecurityUtils.hasRole(Role.ROLE_CUSTOMER)) {
            return SecurityUtils.getCurrentCustomerId();
        }
        return requestedCustomerId;
    }

    private String generateQuotationNumber() {
        return "QT-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 90000) + 10000);
    }

    private QuotationResponse mapToQuotationResponse(Quotation q) {
        List<QuotationItemResponse> itemResponses = q.getItems().stream()
                .map(i -> QuotationItemResponse.builder()
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

        return QuotationResponse.builder()
                .id(q.getId())
                .quotationNumber(q.getQuotationNumber())
                .customerId(q.getCustomer().getId())
                .customerCode(q.getCustomer().getCustomerCode())
                .customerCompanyName(q.getCustomer().getCompanyName())
                .customerEmail(q.getCustomer().getEmail())
                .planId(q.getPlan().getId())
                .planName(q.getPlan().getName())
                .status(q.getStatus())
                .issueDate(q.getIssueDate())
                .expiryDate(q.getExpiryDate())
                .paymentTerms(q.getPaymentTerms())
                .subtotal(q.getSubtotal())
                .taxAmount(q.getTaxAmount())
                .discountAmount(q.getDiscountAmount())
                .totalAmount(q.getTotalAmount())
                .notes(q.getNotes())
                .createdById(q.getCreatedBy() != null ? q.getCreatedBy().getId() : null)
                .createdByName(q.getCreatedBy() != null ? q.getCreatedBy().getFirstName() + " " + q.getCreatedBy().getLastName() : null)
                .items(itemResponses)
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}

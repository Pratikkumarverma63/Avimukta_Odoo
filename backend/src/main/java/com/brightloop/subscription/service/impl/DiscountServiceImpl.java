package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.domain.enums.DiscountType;
import com.brightloop.subscription.repository.*;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.DiscountService;
import com.brightloop.subscription.web.dto.*;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRuleRepository discountRuleRepository;
    private final ProductRepository productRepository;
    private final DiscountUsageRepository discountUsageRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public DiscountServiceImpl(
            DiscountRuleRepository discountRuleRepository,
            ProductRepository productRepository,
            DiscountUsageRepository discountUsageRepository,
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            AuditLogService auditLogService) {
        this.discountRuleRepository = discountRuleRepository;
        this.productRepository = productRepository;
        this.discountUsageRepository = discountUsageRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public DiscountRuleResponse createDiscountRule(DiscountRuleRequest request) {
        if (discountRuleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Discount rule with code '" + request.getCode() + "' already exists.");
        }

        Set<Product> applicableProducts = new HashSet<>();
        if (request.getApplicableProductIds() != null && !request.getApplicableProductIds().isEmpty()) {
            applicableProducts.addAll(productRepository.findAllById(request.getApplicableProductIds()));
        }

        DiscountRule rule = DiscountRule.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minPurchaseAmount(request.getMinPurchaseAmount() != null ? request.getMinPurchaseAmount() : BigDecimal.ZERO)
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxUses(request.getMaxUses() != null ? request.getMaxUses() : 0)
                .usedCount(0)
                .scope(request.getScope())
                .applicableProducts(applicableProducts)
                .active(request.isActive())
                .build();

        rule = discountRuleRepository.save(rule);
        auditLogService.logAction("DISCOUNT_RULE_CREATED", "DiscountRule", rule.getId(), null, rule.getCode());

        return mapToResponse(rule);
    }

    @Override
    public DiscountRuleResponse updateDiscountRule(Long id, DiscountRuleRequest request) {
        DiscountRule rule = discountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount rule not found with id: " + id));

        if (!rule.getCode().equalsIgnoreCase(request.getCode().trim()) &&
                discountRuleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Discount rule with code '" + request.getCode() + "' already exists.");
        }

        rule.setCode(request.getCode().trim().toUpperCase());
        rule.setName(request.getName().trim());
        rule.setDiscountType(request.getDiscountType());
        rule.setDiscountValue(request.getDiscountValue());
        rule.setMinPurchaseAmount(request.getMinPurchaseAmount() != null ? request.getMinPurchaseAmount() : BigDecimal.ZERO);
        rule.setMinQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1);
        rule.setStartDate(request.getStartDate());
        rule.setEndDate(request.getEndDate());
        rule.setMaxUses(request.getMaxUses() != null ? request.getMaxUses() : 0);
        rule.setScope(request.getScope());
        rule.setActive(request.isActive());

        if (request.getApplicableProductIds() != null) {
            rule.getApplicableProducts().clear();
            rule.getApplicableProducts().addAll(productRepository.findAllById(request.getApplicableProductIds()));
        }

        rule = discountRuleRepository.save(rule);
        auditLogService.logAction("DISCOUNT_RULE_UPDATED", "DiscountRule", rule.getId(), null, rule.getCode());

        return mapToResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountRuleResponse getDiscountRuleById(Long id) {
        DiscountRule rule = discountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount rule not found with id: " + id));
        return mapToResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountRuleResponse getDiscountRuleByCode(String code) {
        DiscountRule rule = discountRuleRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount rule not found with code: " + code));
        return mapToResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountRuleResponse> getAllDiscountRules() {
        return discountRuleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountRuleResponse> getActiveDiscountRules() {
        return discountRuleRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResponse validateDiscount(DiscountValidationRequest request) {
        DiscountRule rule = discountRuleRepository.findByCodeIgnoreCase(request.getCode().trim()).orElse(null);

        if (rule == null || !rule.isActive()) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Discount code does not exist or is inactive.")
                    .build();
        }

        LocalDate today = LocalDate.now();
        if (rule.getStartDate() != null && today.isBefore(rule.getStartDate())) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Discount offer has not started yet.")
                    .build();
        }

        if (rule.getEndDate() != null && today.isAfter(rule.getEndDate())) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Discount code has expired.")
                    .build();
        }

        if (rule.getMaxUses() != null && rule.getMaxUses() > 0 && rule.getUsedCount() >= rule.getMaxUses()) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Discount code usage limit has been reached.")
                    .build();
        }

        if (rule.getMinPurchaseAmount() != null && request.getPurchaseAmount().compareTo(rule.getMinPurchaseAmount()) < 0) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Minimum purchase amount of ₹" + rule.getMinPurchaseAmount() + " required.")
                    .build();
        }

        int qty = request.getQuantity() != null ? request.getQuantity() : 1;
        if (rule.getMinQuantity() != null && qty < rule.getMinQuantity()) {
            return DiscountValidationResponse.builder()
                    .eligible(false)
                    .reason("Minimum quantity of " + rule.getMinQuantity() + " required.")
                    .build();
        }

        BigDecimal calculatedDiscount;
        if (rule.getDiscountType() == DiscountType.PERCENTAGE) {
            calculatedDiscount = request.getPurchaseAmount()
                    .multiply(rule.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            calculatedDiscount = rule.getDiscountValue().min(request.getPurchaseAmount());
        }

        return DiscountValidationResponse.builder()
                .eligible(true)
                .reason("Discount is valid.")
                .discountRuleId(rule.getId())
                .code(rule.getCode())
                .discountType(rule.getDiscountType())
                .discountValue(rule.getDiscountValue())
                .calculatedDiscount(calculatedDiscount)
                .build();
    }

    @Override
    public boolean consumeDiscountUsage(Long discountRuleId, Long invoiceId, Long customerId, BigDecimal discountAmount) {
        // Concurrency-safe atomic increment of usedCount in DB
        int rowsUpdated = discountRuleRepository.incrementUsageCountIfAvailable(discountRuleId);
        if (rowsUpdated == 0) {
            return false; // Limit reached or inactive
        }

        DiscountRule rule = discountRuleRepository.findById(discountRuleId).orElse(null);
        Invoice invoice = invoiceId != null ? invoiceRepository.findById(invoiceId).orElse(null) : null;
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;

        DiscountUsage usage = DiscountUsage.builder()
                .discountRule(rule)
                .invoice(invoice)
                .customer(customer)
                .discountAmount(discountAmount)
                .build();

        discountUsageRepository.save(usage);
        return true;
    }

    @Override
    public void deleteDiscountRule(Long id) {
        DiscountRule rule = discountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount rule not found with id: " + id));
        rule.setActive(false);
        discountRuleRepository.save(rule);
        auditLogService.logAction("DISCOUNT_RULE_DEACTIVATED", "DiscountRule", rule.getId(), null, rule.getCode());
    }

    private DiscountRuleResponse mapToResponse(DiscountRule r) {
        Integer remaining = null;
        if (r.getMaxUses() != null && r.getMaxUses() > 0) {
            remaining = Math.max(0, r.getMaxUses() - r.getUsedCount());
        }

        Set<Long> productIds = r.getApplicableProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return DiscountRuleResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(r.getName())
                .discountType(r.getDiscountType())
                .discountValue(r.getDiscountValue())
                .minPurchaseAmount(r.getMinPurchaseAmount())
                .minQuantity(r.getMinQuantity())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .maxUses(r.getMaxUses())
                .usedCount(r.getUsedCount())
                .remainingUses(remaining)
                .scope(r.getScope())
                .applicableProductIds(productIds)
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

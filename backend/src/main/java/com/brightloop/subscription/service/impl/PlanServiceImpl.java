package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.Plan;
import com.brightloop.subscription.domain.entity.Product;
import com.brightloop.subscription.repository.PlanRepository;
import com.brightloop.subscription.repository.ProductRepository;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.PlanService;
import com.brightloop.subscription.util.BillingDateCalculator;
import com.brightloop.subscription.web.dto.PlanRequest;
import com.brightloop.subscription.web.dto.PlanResponse;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public PlanServiceImpl(
            PlanRepository planRepository,
            ProductRepository productRepository,
            AuditLogService auditLogService) {
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public PlanResponse createPlan(PlanRequest request) {
        if (planRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Plan with code '" + request.getCode() + "' already exists.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Plan plan = Plan.builder()
                .product(product)
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .price(request.getPrice())
                .billingFrequency(request.getBillingFrequency())
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .availabilityStartDate(request.getAvailabilityStartDate())
                .availabilityEndDate(request.getAvailabilityEndDate())
                .autoCloseOnExpiry(request.isAutoCloseOnExpiry())
                .allowPause(request.isAllowPause())
                .allowEarlyClosure(request.isAllowEarlyClosure())
                .allowRenewal(request.isAllowRenewal())
                .active(request.isActive())
                .build();

        plan = planRepository.save(plan);
        auditLogService.logAction("PLAN_CREATED", "Plan", plan.getId(), null, plan.getCode());

        return mapToPlanResponse(plan);
    }

    @Override
    public PlanResponse updatePlan(Long id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (!plan.getCode().equalsIgnoreCase(request.getCode().trim()) &&
                planRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Plan with code '" + request.getCode() + "' already exists.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        plan.setProduct(product);
        plan.setName(request.getName().trim());
        plan.setCode(request.getCode().trim().toUpperCase());
        plan.setPrice(request.getPrice());
        plan.setBillingFrequency(request.getBillingFrequency());
        plan.setMinQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1);
        plan.setAvailabilityStartDate(request.getAvailabilityStartDate());
        plan.setAvailabilityEndDate(request.getAvailabilityEndDate());
        plan.setAutoCloseOnExpiry(request.isAutoCloseOnExpiry());
        plan.setAllowPause(request.isAllowPause());
        plan.setAllowEarlyClosure(request.isAllowEarlyClosure());
        plan.setAllowRenewal(request.isAllowRenewal());
        plan.setActive(request.isActive());

        plan = planRepository.save(plan);
        auditLogService.logAction("PLAN_UPDATED", "Plan", plan.getId(), null, plan.getCode());

        return mapToPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        return mapToPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlanByCode(String code) {
        Plan plan = planRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with code: " + code));
        return mapToPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlanResponse> getAllPlans(Pageable pageable, Boolean activeOnly, Long productId) {
        Page<Plan> page;
        if (Boolean.TRUE.equals(activeOnly)) {
            page = planRepository.findByActive(true, pageable);
        } else {
            page = planRepository.findAll(pageable);
        }
        return page.map(this::mapToPlanResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .map(this::mapToPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate calculateNextBillingDate(Long planId, LocalDate fromDate) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));
        return BillingDateCalculator.calculateNextBillingDate(fromDate, plan.getBillingFrequency());
    }

    private PlanResponse mapToPlanResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .productId(plan.getProduct().getId())
                .productCode(plan.getProduct().getCode())
                .productName(plan.getProduct().getName())
                .name(plan.getName())
                .code(plan.getCode())
                .price(plan.getPrice())
                .billingFrequency(plan.getBillingFrequency())
                .minQuantity(plan.getMinQuantity())
                .availabilityStartDate(plan.getAvailabilityStartDate())
                .availabilityEndDate(plan.getAvailabilityEndDate())
                .autoCloseOnExpiry(plan.isAutoCloseOnExpiry())
                .allowPause(plan.isAllowPause())
                .allowEarlyClosure(plan.isAllowEarlyClosure())
                .allowRenewal(plan.isAllowRenewal())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}

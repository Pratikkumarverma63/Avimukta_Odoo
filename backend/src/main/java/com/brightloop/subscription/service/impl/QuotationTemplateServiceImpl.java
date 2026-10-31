package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.*;
import com.brightloop.subscription.repository.PlanRepository;
import com.brightloop.subscription.repository.ProductRepository;
import com.brightloop.subscription.repository.ProductVariantRepository;
import com.brightloop.subscription.repository.QuotationTemplateRepository;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.PricingCalculationService;
import com.brightloop.subscription.service.QuotationTemplateService;
import com.brightloop.subscription.web.dto.QuotationTemplateRequest;
import com.brightloop.subscription.web.dto.QuotationTemplateResponse;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuotationTemplateServiceImpl implements QuotationTemplateService {

    private final QuotationTemplateRepository templateRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PricingCalculationService pricingCalculationService;
    private final AuditLogService auditLogService;

    public QuotationTemplateServiceImpl(
            QuotationTemplateRepository templateRepository,
            PlanRepository planRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            PricingCalculationService pricingCalculationService,
            AuditLogService auditLogService) {
        this.templateRepository = templateRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.pricingCalculationService = pricingCalculationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public QuotationTemplateResponse createTemplate(QuotationTemplateRequest request) {
        Plan defaultPlan = null;
        if (request.getDefaultPlanId() != null) {
            defaultPlan = planRepository.findById(request.getDefaultPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.getDefaultPlanId()));
        }

        QuotationTemplate template = QuotationTemplate.builder()
                .name(request.getName().trim())
                .defaultPlan(defaultPlan)
                .validityDays(request.getValidityDays() != null ? request.getValidityDays() : 30)
                .active(request.isActive())
                .build();

        if (request.getItems() != null) {
            for (QuotationTemplateRequest.TemplateItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

                ProductVariant variant = null;
                if (itemReq.getVariantId() != null) {
                    variant = variantRepository.findById(itemReq.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + itemReq.getVariantId()));
                }

                BigDecimal unitPrice = pricingCalculationService.resolveUnitPrice(product, variant, itemReq.getUnitPrice());

                QuotationTemplateItem item = QuotationTemplateItem.builder()
                        .product(product)
                        .variant(variant)
                        .defaultQuantity(itemReq.getDefaultQuantity() != null ? itemReq.getDefaultQuantity() : 1)
                        .unitPrice(unitPrice)
                        .build();

                template.addItem(item);
            }
        }

        template = templateRepository.save(template);
        auditLogService.logAction("TEMPLATE_CREATED", "QuotationTemplate", template.getId(), null, template.getName());

        return mapToResponse(template);
    }

    @Override
    public QuotationTemplateResponse updateTemplate(Long id, QuotationTemplateRequest request) {
        QuotationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        Plan defaultPlan = null;
        if (request.getDefaultPlanId() != null) {
            defaultPlan = planRepository.findById(request.getDefaultPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + request.getDefaultPlanId()));
        }

        template.setName(request.getName().trim());
        template.setDefaultPlan(defaultPlan);
        template.setValidityDays(request.getValidityDays() != null ? request.getValidityDays() : 30);
        template.setActive(request.isActive());

        template.getItems().clear();
        if (request.getItems() != null) {
            for (QuotationTemplateRequest.TemplateItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

                ProductVariant variant = null;
                if (itemReq.getVariantId() != null) {
                    variant = variantRepository.findById(itemReq.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + itemReq.getVariantId()));
                }

                BigDecimal unitPrice = pricingCalculationService.resolveUnitPrice(product, variant, itemReq.getUnitPrice());

                QuotationTemplateItem item = QuotationTemplateItem.builder()
                        .product(product)
                        .variant(variant)
                        .defaultQuantity(itemReq.getDefaultQuantity() != null ? itemReq.getDefaultQuantity() : 1)
                        .unitPrice(unitPrice)
                        .build();

                template.addItem(item);
            }
        }

        template = templateRepository.save(template);
        auditLogService.logAction("TEMPLATE_UPDATED", "QuotationTemplate", template.getId(), null, template.getName());

        return mapToResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationTemplateResponse getTemplateById(Long id) {
        QuotationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        return mapToResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotationTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTemplate(Long id) {
        QuotationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        template.setActive(false);
        templateRepository.save(template);
        auditLogService.logAction("TEMPLATE_DEACTIVATED", "QuotationTemplate", template.getId(), null, template.getName());
    }

    private QuotationTemplateResponse mapToResponse(QuotationTemplate template) {
        List<QuotationTemplateResponse.TemplateItemResponse> itemResponses = template.getItems().stream()
                .map(i -> QuotationTemplateResponse.TemplateItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productCode(i.getProduct().getCode())
                        .productName(i.getProduct().getName())
                        .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                        .variantName(i.getVariant() != null ? i.getVariant().getAttributeName() + ": " + i.getVariant().getVariantValue() : null)
                        .defaultQuantity(i.getDefaultQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return QuotationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .defaultPlanId(template.getDefaultPlan() != null ? template.getDefaultPlan().getId() : null)
                .defaultPlanName(template.getDefaultPlan() != null ? template.getDefaultPlan().getName() : null)
                .validityDays(template.getValidityDays())
                .active(template.isActive())
                .items(itemResponses)
                .createdAt(template.getCreatedAt())
                .build();
    }
}

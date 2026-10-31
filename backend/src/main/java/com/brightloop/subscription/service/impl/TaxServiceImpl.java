package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.TaxRule;
import com.brightloop.subscription.repository.TaxRuleRepository;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.TaxService;
import com.brightloop.subscription.web.dto.TaxRuleRequest;
import com.brightloop.subscription.web.dto.TaxRuleResponse;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaxServiceImpl implements TaxService {

    private final TaxRuleRepository taxRuleRepository;
    private final AuditLogService auditLogService;

    public TaxServiceImpl(TaxRuleRepository taxRuleRepository, AuditLogService auditLogService) {
        this.taxRuleRepository = taxRuleRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public TaxRuleResponse createTaxRule(TaxRuleRequest request) {
        if (taxRuleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Tax rule with code '" + request.getCode() + "' already exists.");
        }

        TaxRule taxRule = TaxRule.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .ratePercent(request.getRatePercent())
                .isPercentage(request.isPercentage())
                .applicability(request.getApplicability() != null ? request.getApplicability() : "ALL")
                .effectiveStartDate(request.getEffectiveStartDate())
                .effectiveEndDate(request.getEffectiveEndDate())
                .active(request.isActive())
                .build();

        taxRule = taxRuleRepository.save(taxRule);
        auditLogService.logAction("TAX_RULE_CREATED", "TaxRule", taxRule.getId(), null, taxRule.getCode());

        return mapToResponse(taxRule);
    }

    @Override
    public TaxRuleResponse updateTaxRule(Long id, TaxRuleRequest request) {
        TaxRule taxRule = taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));

        if (!taxRule.getCode().equalsIgnoreCase(request.getCode().trim()) &&
                taxRuleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Tax rule with code '" + request.getCode() + "' already exists.");
        }

        taxRule.setName(request.getName().trim());
        taxRule.setCode(request.getCode().trim().toUpperCase());
        taxRule.setRatePercent(request.getRatePercent());
        taxRule.setPercentage(request.isPercentage());
        taxRule.setApplicability(request.getApplicability());
        taxRule.setEffectiveStartDate(request.getEffectiveStartDate());
        taxRule.setEffectiveEndDate(request.getEffectiveEndDate());
        taxRule.setActive(request.isActive());

        taxRule = taxRuleRepository.save(taxRule);
        auditLogService.logAction("TAX_RULE_UPDATED", "TaxRule", taxRule.getId(), null, taxRule.getCode());

        return mapToResponse(taxRule);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxRuleResponse getTaxRuleById(Long id) {
        TaxRule taxRule = taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));
        return mapToResponse(taxRule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleResponse> getAllTaxRules() {
        return taxRuleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleResponse> getActiveTaxRules() {
        return taxRuleRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTaxRule(Long id) {
        TaxRule taxRule = taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));
        taxRule.setActive(false);
        taxRuleRepository.save(taxRule);
        auditLogService.logAction("TAX_RULE_DEACTIVATED", "TaxRule", taxRule.getId(), null, taxRule.getCode());
    }

    private TaxRuleResponse mapToResponse(TaxRule t) {
        return TaxRuleResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .code(t.getCode())
                .ratePercent(t.getRatePercent())
                .percentage(t.isPercentage())
                .applicability(t.getApplicability())
                .effectiveStartDate(t.getEffectiveStartDate())
                .effectiveEndDate(t.getEffectiveEndDate())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .build();
    }
}

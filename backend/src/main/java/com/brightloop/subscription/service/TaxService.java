package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.TaxRuleRequest;
import com.brightloop.subscription.web.dto.TaxRuleResponse;

import java.util.List;

public interface TaxService {
    TaxRuleResponse createTaxRule(TaxRuleRequest request);
    TaxRuleResponse updateTaxRule(Long id, TaxRuleRequest request);
    TaxRuleResponse getTaxRuleById(Long id);
    List<TaxRuleResponse> getAllTaxRules();
    List<TaxRuleResponse> getActiveTaxRules();
    void deleteTaxRule(Long id);
}

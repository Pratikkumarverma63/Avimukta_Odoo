package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.*;

import java.util.List;

public interface DiscountService {
    DiscountRuleResponse createDiscountRule(DiscountRuleRequest request);
    DiscountRuleResponse updateDiscountRule(Long id, DiscountRuleRequest request);
    DiscountRuleResponse getDiscountRuleById(Long id);
    DiscountRuleResponse getDiscountRuleByCode(String code);
    List<DiscountRuleResponse> getAllDiscountRules();
    List<DiscountRuleResponse> getActiveDiscountRules();
    DiscountValidationResponse validateDiscount(DiscountValidationRequest request);
    boolean consumeDiscountUsage(Long discountRuleId, Long invoiceId, Long customerId, java.math.BigDecimal discountAmount);
    void deleteDiscountRule(Long id);
}

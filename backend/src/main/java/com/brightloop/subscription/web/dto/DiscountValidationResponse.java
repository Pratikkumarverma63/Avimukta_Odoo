package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountValidationResponse {
    private boolean eligible;
    private String reason;
    private Long discountRuleId;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal calculatedDiscount;
}

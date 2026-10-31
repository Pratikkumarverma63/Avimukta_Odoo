package com.brightloop.subscription.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Long variantId;
    private String variantName;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long taxRuleId;
    private String taxRuleName;
    private BigDecimal taxAmount;
    private Long discountRuleId;
    private String discountRuleName;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
}

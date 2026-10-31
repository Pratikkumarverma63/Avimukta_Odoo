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
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String attributeName;
    private String variantValue;
    private BigDecimal priceAdjustment;
    private BigDecimal effectivePrice; // Base price + priceAdjustment
    private String sku;
    private boolean active;
}

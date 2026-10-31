package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    @NotBlank(message = "Attribute name is required (e.g. Brand, Tier, Edition)")
    private String attributeName;

    @NotBlank(message = "Variant value is required (e.g. Odoo, Enterprise)")
    private String variantValue;

    @NotNull(message = "Price adjustment is required")
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    private String sku;

    @Builder.Default
    private boolean active = true;
}

package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.Min;
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
public class QuotationItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId;

    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private BigDecimal unitPrice; // Optional: calculated from product+variant if null

    private Long taxRuleId;
    private Long discountRuleId;
}

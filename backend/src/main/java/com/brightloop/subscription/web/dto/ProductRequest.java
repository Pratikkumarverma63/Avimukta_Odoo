package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
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
public class ProductRequest {

    @NotBlank(message = "Product code is required")
    private String code;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @NotNull(message = "Sales price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Sales price cannot be negative")
    private BigDecimal salesPrice;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    private String description;

    @Builder.Default
    private boolean active = true;
}

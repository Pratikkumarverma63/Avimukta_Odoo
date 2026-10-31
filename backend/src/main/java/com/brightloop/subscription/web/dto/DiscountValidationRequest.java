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
public class DiscountValidationRequest {

    @NotBlank(message = "Discount code is required")
    private String code;

    @NotNull(message = "Purchase amount is required")
    private BigDecimal purchaseAmount;

    @Builder.Default
    private Integer quantity = 1;

    private Long productId;
}

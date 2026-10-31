package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.DiscountScope;
import com.brightloop.subscription.domain.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRuleRequest {

    @NotBlank(message = "Discount code is required")
    private String code;

    @NotBlank(message = "Discount name is required")
    private String name;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    @Builder.Default
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @Builder.Default
    private Integer minQuantity = 1;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private Integer maxUses = 0; // 0 = unlimited

    @Builder.Default
    private DiscountScope scope = DiscountScope.SUBSCRIPTION_LEVEL;

    @Builder.Default
    private Set<Long> applicableProductIds = new HashSet<>();

    @Builder.Default
    private boolean active = true;
}

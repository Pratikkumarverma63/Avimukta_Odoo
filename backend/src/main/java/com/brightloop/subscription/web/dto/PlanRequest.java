package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.BillingFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Plan name is required")
    private String name;

    @NotBlank(message = "Plan code is required")
    private String code;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Billing frequency is required")
    private BillingFrequency billingFrequency;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 1, message = "Minimum quantity must be at least 1")
    @Builder.Default
    private Integer minQuantity = 1;

    private LocalDate availabilityStartDate;
    private LocalDate availabilityEndDate;

    @Builder.Default
    private boolean autoCloseOnExpiry = true;

    @Builder.Default
    private boolean allowPause = true;

    @Builder.Default
    private boolean allowEarlyClosure = true;

    @Builder.Default
    private boolean allowRenewal = true;

    @Builder.Default
    private boolean active = true;
}

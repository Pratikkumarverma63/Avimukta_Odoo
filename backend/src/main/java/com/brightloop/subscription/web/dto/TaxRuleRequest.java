package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class TaxRuleRequest {

    @NotBlank(message = "Tax name is required")
    private String name;

    @NotBlank(message = "Tax code is required")
    private String code;

    @NotNull(message = "Rate percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate percent cannot be negative")
    private BigDecimal ratePercent;

    @Builder.Default
    private boolean percentage = true;

    @Builder.Default
    private String applicability = "ALL";

    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    @Builder.Default
    private boolean active = true;
}

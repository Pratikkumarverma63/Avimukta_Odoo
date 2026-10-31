package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrationCalculationRequest {
    @NotNull(message = "New target plan ID is required")
    private Long newPlanId;

    private Long newVariantId;

    private LocalDate effectiveDate; // defaults to today
}

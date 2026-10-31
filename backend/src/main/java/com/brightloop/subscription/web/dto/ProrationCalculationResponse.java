package com.brightloop.subscription.web.dto;

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
public class ProrationCalculationResponse {
    private Long subscriptionId;
    private String subscriptionNumber;
    private String currentPlanName;
    private String targetPlanName;
    private LocalDate effectiveDate;
    private int daysUsed;
    private int daysRemaining;
    private int totalCycleDays;
    private BigDecimal currentPlanAmount;
    private BigDecimal targetPlanAmount;
    private BigDecimal unutilizedCreditAmount;
    private BigDecimal proratedChargeAmount;
    private BigDecimal netAdjustmentAmount; // Positive = customer pays difference; Negative = credit balance
    private String summaryText;
}

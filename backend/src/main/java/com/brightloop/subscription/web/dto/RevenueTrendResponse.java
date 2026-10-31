package com.brightloop.subscription.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTrendResponse {

    @Builder.Default
    private List<MonthlyTrendPoint> monthlyPoints = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendPoint {
        private String month; // e.g. "2026-01"
        private BigDecimal invoicedAmount;
        private BigDecimal collectedAmount;
        private BigDecimal outstandingAmount;
        private long activeSubscriptionsCount;
    }
}

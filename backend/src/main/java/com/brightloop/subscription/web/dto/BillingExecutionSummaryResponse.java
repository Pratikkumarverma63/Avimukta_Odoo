package com.brightloop.subscription.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingExecutionSummaryResponse {
    private LocalDate executionDate;
    private int subscriptionsProcessed;
    private int invoicesGenerated;
    private int skippedCount;
    private int failedCount;
    private BigDecimal totalBilledAmount;
    @Builder.Default
    private List<BillingLogEntry> entries = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingLogEntry {
        private Long subscriptionId;
        private String subscriptionNumber;
        private String customerName;
        private String invoiceNumber;
        private BigDecimal billedAmount;
        private String status;
        private String message;
    }
}

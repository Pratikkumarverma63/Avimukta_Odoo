package com.brightloop.subscription.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {
    private long totalCustomers;
    private long activeSubscriptions;
    private long totalSubscriptions;
    private BigDecimal monthlyRecurringRevenue; // MRR
    private BigDecimal annualRunRate;           // ARR
    private BigDecimal totalInvoicedRevenue;
    private BigDecimal totalPaymentsReceived;
    private BigDecimal totalOutstandingAmount;
    private long overdueInvoicesCount;
    private BigDecimal overdueInvoicesAmount;
    private long upcomingRenewalsCount;

    @Builder.Default
    private Map<String, Long> subscriptionsByStatus = new HashMap<>();

    @Builder.Default
    private Map<String, Long> invoicesByStatus = new HashMap<>();

    @Builder.Default
    private Map<String, BigDecimal> paymentsByMethod = new HashMap<>();
}

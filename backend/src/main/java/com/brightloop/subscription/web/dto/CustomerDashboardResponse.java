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
public class CustomerDashboardResponse {
    private String customerCode;
    private String companyName;
    private String contactName;
    private String email;
    private String phone;
    private String billingAddress;
    private String taxId;

    private int activeSubscriptionsCount;
    private BigDecimal totalInvoicedAmount;
    private BigDecimal totalAmountPaid;
    private BigDecimal totalOutstandingAmount;
    private LocalDate nextUpcomingBillingDate;
    private long unreadNotificationCount;

    @Builder.Default
    private List<SubscriptionResponse> activeSubscriptions = new ArrayList<>();

    @Builder.Default
    private List<InvoiceResponse> recentInvoices = new ArrayList<>();

    @Builder.Default
    private List<PaymentResponse> recentPayments = new ArrayList<>();

    @Builder.Default
    private List<NotificationResponse> unreadNotifications = new ArrayList<>();

    public BigDecimal getTotalOutstandingBalance() {
        return totalOutstandingAmount != null ? totalOutstandingAmount : BigDecimal.ZERO;
    }

    public LocalDate getNextRenewalDate() {
        return nextUpcomingBillingDate;
    }
}

package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.BillingFrequency;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private String subscriptionNumber;
    private Long customerId;
    private String customerCode;
    private String customerCompanyName;
    private String customerEmail;
    private Long planId;
    private String planName;
    private String planCode;
    private BillingFrequency billingFrequency;
    private Long quotationId;
    private String quotationNumber;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private LocalDate nextBillingDate;
    private LocalDate lastBilledDate;
    private String paymentTerms;
    private boolean autoCloseOnExpiry;
    private boolean allowPause;
    private boolean allowEarlyClosure;
    private boolean allowRenewal;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    @Builder.Default
    private List<SubscriptionItemResponse> items = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private Long quotationId;

    private LocalDate startDate;
    private LocalDate expiryDate;

    @Builder.Default
    private String paymentTerms = "NET_30";

    @Builder.Default
    private boolean autoCloseOnExpiry = true;

    @Builder.Default
    private boolean allowPause = true;

    @Builder.Default
    private boolean allowEarlyClosure = true;

    @Builder.Default
    private boolean allowRenewal = true;

    @NotEmpty(message = "At least one subscription item is required")
    @Builder.Default
    private List<SubscriptionItemRequest> items = new ArrayList<>();
}

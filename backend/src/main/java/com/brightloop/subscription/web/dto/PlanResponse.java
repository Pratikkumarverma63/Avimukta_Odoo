package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.BillingFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String name;
    private String code;
    private BigDecimal price;
    private BillingFrequency billingFrequency;
    private Integer minQuantity;
    private LocalDate availabilityStartDate;
    private LocalDate availabilityEndDate;
    private boolean autoCloseOnExpiry;
    private boolean allowPause;
    private boolean allowEarlyClosure;
    private boolean allowRenewal;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

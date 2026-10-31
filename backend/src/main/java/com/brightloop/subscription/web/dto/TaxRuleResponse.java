package com.brightloop.subscription.web.dto;

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
public class TaxRuleResponse {
    private Long id;
    private String name;
    private String code;
    private BigDecimal ratePercent;
    private boolean percentage;
    private String applicability;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private boolean active;
    private OffsetDateTime createdAt;
}

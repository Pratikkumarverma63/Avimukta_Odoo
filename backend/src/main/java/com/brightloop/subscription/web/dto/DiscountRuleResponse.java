package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.DiscountScope;
import com.brightloop.subscription.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRuleResponse {
    private Long id;
    private String code;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minPurchaseAmount;
    private Integer minQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxUses;
    private Integer usedCount;
    private Integer remainingUses;
    private DiscountScope scope;
    @Builder.Default
    private Set<Long> applicableProductIds = new HashSet<>();
    private boolean active;
    private OffsetDateTime createdAt;
}

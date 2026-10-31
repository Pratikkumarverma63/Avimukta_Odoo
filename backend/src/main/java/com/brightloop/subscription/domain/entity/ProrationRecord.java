package com.brightloop.subscription.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "proration_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // PLAN_UPGRADE, PLAN_DOWNGRADE, CYCLE_PAUSE, EARLY_TERMINATION

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "old_plan_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal oldPlanPrice;

    @Column(name = "new_plan_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal newPlanPrice;

    @Column(name = "days_used", nullable = false)
    private Integer daysUsed;

    @Column(name = "days_in_cycle", nullable = false)
    private Integer daysInCycle;

    @Column(name = "prorated_credit_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal proratedCreditAmount = BigDecimal.ZERO;

    @Column(name = "prorated_charge_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal proratedChargeAmount = BigDecimal.ZERO;

    @Column(name = "net_adjustment_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal netAdjustmentAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

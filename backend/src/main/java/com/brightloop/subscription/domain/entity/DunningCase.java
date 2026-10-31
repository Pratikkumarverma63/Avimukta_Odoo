package com.brightloop.subscription.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dunning_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DunningCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "dunning_stage", nullable = false, length = 50)
    @Builder.Default
    private String dunningStage = "HEALTHY"; // HEALTHY, REMINDER_SENT, GRACE_PERIOD, LATE_FEE_APPLIED, SUSPENDED, WRITTEN_OFF

    @Column(name = "days_overdue", nullable = false)
    @Builder.Default
    private Integer daysOverdue = 0;

    @Column(name = "late_fee_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "last_action_date")
    private OffsetDateTime lastActionDate;

    @Column(name = "next_action_date")
    private OffsetDateTime nextActionDate;

    @Column(name = "attempts_count", nullable = false)
    @Builder.Default
    private Integer attemptsCount = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

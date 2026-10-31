package com.brightloop.subscription.domain.entity;

import com.brightloop.subscription.domain.enums.BillingJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "billing_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_sub_billing_period",
            columnNames = {"subscription_id", "billing_period_start", "billing_period_end"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BillingJobStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_invoice_id")
    private Invoice generatedInvoice;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

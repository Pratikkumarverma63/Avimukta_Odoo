package com.brightloop.subscription.domain.entity;

import com.brightloop.subscription.domain.enums.BillingFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency", nullable = false, length = 50)
    private BillingFrequency billingFrequency; // DAILY, WEEKLY, MONTHLY, YEARLY

    @Column(name = "min_quantity", nullable = false)
    @Builder.Default
    private Integer minQuantity = 1;

    @Column(name = "availability_start_date")
    private LocalDate availabilityStartDate;

    @Column(name = "availability_end_date")
    private LocalDate availabilityEndDate;

    @Column(name = "auto_close_on_expiry", nullable = false)
    @Builder.Default
    private boolean autoCloseOnExpiry = true;

    @Column(name = "allow_pause", nullable = false)
    @Builder.Default
    private boolean allowPause = true;

    @Column(name = "allow_early_closure", nullable = false)
    @Builder.Default
    private boolean allowEarlyClosure = true;

    @Column(name = "allow_renewal", nullable = false)
    @Builder.Default
    private boolean allowRenewal = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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

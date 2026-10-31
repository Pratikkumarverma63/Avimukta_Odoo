package com.brightloop.subscription.domain.entity;

import com.brightloop.subscription.domain.enums.DiscountScope;
import com.brightloop.subscription.domain.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "discount_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 50)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_purchase_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @Column(name = "min_quantity")
    @Builder.Default
    private Integer minQuantity = 1;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_uses")
    @Builder.Default
    private Integer maxUses = 0; // 0 = unlimited

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DiscountScope scope = DiscountScope.SUBSCRIPTION_LEVEL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "discount_rule_products",
        joinColumns = @JoinColumn(name = "discount_rule_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private Set<Product> applicableProducts = new HashSet<>();

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

    public boolean isEligible(BigDecimal subtotal, int quantity, LocalDate currentDate) {
        if (!active) return false;
        if (startDate != null && currentDate.isBefore(startDate)) return false;
        if (endDate != null && currentDate.isAfter(endDate)) return false;
        if (maxUses != null && maxUses > 0 && usedCount >= maxUses) return false;
        if (minPurchaseAmount != null && subtotal.compareTo(minPurchaseAmount) < 0) return false;
        if (minQuantity != null && quantity < minQuantity) return false;
        return true;
    }
}

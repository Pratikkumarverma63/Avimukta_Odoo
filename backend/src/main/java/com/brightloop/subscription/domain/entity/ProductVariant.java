package com.brightloop.subscription.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName; // e.g. "Brand", "Edition"

    @Column(name = "variant_value", nullable = false, length = 100)
    private String variantValue;  // e.g. "Odoo", "Enterprise"

    @Column(name = "price_adjustment", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO; // e.g. +560.00

    @Column(unique = true, length = 100)
    private String sku;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

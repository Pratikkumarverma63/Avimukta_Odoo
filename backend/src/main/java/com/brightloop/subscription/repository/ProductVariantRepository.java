package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);
    Optional<ProductVariant> findBySku(String sku);
}

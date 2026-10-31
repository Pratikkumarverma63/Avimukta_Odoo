package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.DiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    Optional<DiscountRule> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<DiscountRule> findByActiveTrue();

    // Concurrency-safe atomic increment of usedCount if maxUses == 0 or usedCount < maxUses
    @Modifying
    @Query("UPDATE DiscountRule d SET d.usedCount = d.usedCount + 1 " +
           "WHERE d.id = :id AND (d.maxUses = 0 OR d.usedCount < d.maxUses)")
    int incrementUsageCountIfAvailable(@Param("id") Long id);
}

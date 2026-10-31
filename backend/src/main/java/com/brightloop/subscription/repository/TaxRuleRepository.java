package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {
    Optional<TaxRule> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<TaxRule> findByActiveTrue();
}

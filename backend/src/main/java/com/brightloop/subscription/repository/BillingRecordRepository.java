package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.BillingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {
    Optional<BillingRecord> findBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
            Long subscriptionId, LocalDate billingPeriodStart, LocalDate billingPeriodEnd);
    boolean existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
            Long subscriptionId, LocalDate billingPeriodStart, LocalDate billingPeriodEnd);
    Page<BillingRecord> findBySubscriptionId(Long subscriptionId, Pageable pageable);
}

package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.ProrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProrationRecordRepository extends JpaRepository<ProrationRecord, Long> {
    List<ProrationRecord> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
    List<ProrationRecord> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

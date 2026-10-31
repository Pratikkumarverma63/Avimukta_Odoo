package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountUsageRepository extends JpaRepository<DiscountUsage, Long> {
    List<DiscountUsage> findByDiscountRuleId(Long discountRuleId);
    List<DiscountUsage> findByInvoiceId(Long invoiceId);
    List<DiscountUsage> findByCustomerId(Long customerId);
}

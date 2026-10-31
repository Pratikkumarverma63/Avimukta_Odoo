package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.DunningCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DunningCaseRepository extends JpaRepository<DunningCase, Long> {
    Optional<DunningCase> findByInvoiceId(Long invoiceId);
    List<DunningCase> findByDunningStageNotOrderByDaysOverdueDesc(String stage);
    List<DunningCase> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

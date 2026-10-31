package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Quotation;
import com.brightloop.subscription.domain.enums.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    Optional<Quotation> findByQuotationNumber(String quotationNumber);
    boolean existsByQuotationNumber(String quotationNumber);
    Page<Quotation> findByCustomerId(Long customerId, Pageable pageable);
    Page<Quotation> findByStatus(QuotationStatus status, Pageable pageable);
    Page<Quotation> findByCustomerIdAndStatus(Long customerId, QuotationStatus status, Pageable pageable);

    @Query("SELECT q FROM Quotation q WHERE q.status = 'QUOTATION' AND q.expiryDate < :currentDate")
    List<Quotation> findExpiredQuotations(@Param("currentDate") LocalDate currentDate);
}

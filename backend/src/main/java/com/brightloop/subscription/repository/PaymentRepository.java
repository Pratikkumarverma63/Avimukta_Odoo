package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    boolean existsByPaymentNumber(String paymentNumber);
    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("SELECT p FROM Payment p JOIN p.invoice i WHERE i.customer.id = :customerId")
    Page<Payment> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN p.invoice i WHERE i.customer.id = :customerId ORDER BY p.id DESC")
    List<Payment> findAllByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaymentsReceived();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN p.invoice i WHERE i.customer.id = :customerId AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaymentsReceivedForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal calculatePaymentsReceivedBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);
}

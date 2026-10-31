package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Invoice;
import com.brightloop.subscription.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
    Page<Invoice> findByCustomerId(Long customerId, Pageable pageable);
    List<Invoice> findAllByCustomerIdOrderByIdDesc(Long customerId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    Page<Invoice> findByCustomerIdAndStatus(Long customerId, InvoiceStatus status, Pageable pageable);
    List<Invoice> findBySubscriptionId(Long subscriptionId);

    @Query("SELECT i FROM Invoice i WHERE i.status <> 'PAID' AND i.status <> 'CANCELLED' AND i.dueDate < :currentDate")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status <> 'PAID' AND i.status <> 'CANCELLED' AND i.dueDate < :currentDate")
    List<Invoice> findOverdueInvoicesForCustomer(@Param("customerId") Long customerId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status <> 'CANCELLED'")
    BigDecimal calculateTotalInvoicedRevenue();

    @Query("SELECT COALESCE(SUM(i.outstandingAmount), 0) FROM Invoice i WHERE i.status <> 'CANCELLED' AND i.status <> 'PAID'")
    BigDecimal calculateTotalOutstanding();

    @Query("SELECT COALESCE(SUM(i.outstandingAmount), 0) FROM Invoice i WHERE i.customer.id = :customerId AND i.status <> 'CANCELLED' AND i.status <> 'PAID'")
    BigDecimal calculateTotalOutstandingForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate AND i.status <> 'CANCELLED'")
    BigDecimal calculateInvoicedRevenueBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

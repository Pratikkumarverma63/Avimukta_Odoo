package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Customer;
import com.brightloop.subscription.domain.entity.Subscription;
import com.brightloop.subscription.domain.enums.SubscriptionStatus;
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
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);
    boolean existsBySubscriptionNumber(String subscriptionNumber);

    List<Subscription> findByCustomerOrderByIdDesc(Customer customer);
    List<Subscription> findAllByCustomerId(Long customerId);

    @Query("SELECT s FROM Subscription s WHERE s.customer.id = :customerId")
    Page<Subscription> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    @Query("SELECT s FROM Subscription s WHERE s.customer.id = :customerId AND s.status = :status")
    Page<Subscription> findByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") SubscriptionStatus status, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Subscription s JOIN FETCH s.customer JOIN FETCH s.plan WHERE s.status = :status AND s.nextBillingDate <= :dueDate")
    List<Subscription> findSubscriptionsDueForBilling(@Param("status") SubscriptionStatus status, @Param("dueDate") LocalDate dueDate);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.autoCloseOnExpiry = true AND s.expiryDate IS NOT NULL AND s.expiryDate < :currentDate")
    List<Subscription> findExpiredSubscriptionsForAutoClose(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.expiryDate BETWEEN :startDate AND :endDate")
    List<Subscription> findUpcomingRenewals(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countByStatus(SubscriptionStatus status);
}

package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    List<Notification> findByCustomerIdAndReadFalse(Long customerId);
    long countByCustomerIdAndReadFalse(Long customerId);
}

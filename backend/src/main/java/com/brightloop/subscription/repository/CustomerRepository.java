package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByEmailIgnoreCase(String email);
    boolean existsByCustomerCode(String customerCode);
    boolean existsByEmailIgnoreCase(String email);
    Page<Customer> findByCompanyNameContainingIgnoreCaseOrContactNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String companyName, String contactName, String email, Pageable pageable);
}

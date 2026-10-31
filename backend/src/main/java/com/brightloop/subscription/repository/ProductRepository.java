package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    Page<Product> findByActive(boolean active, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}

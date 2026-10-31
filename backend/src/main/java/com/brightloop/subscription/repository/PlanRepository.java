package com.brightloop.subscription.repository;

import com.brightloop.subscription.domain.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Plan> findByActiveTrue();
    Page<Plan> findByActive(boolean active, Pageable pageable);
    List<Plan> findByProductIdAndActiveTrue(Long productId);
}

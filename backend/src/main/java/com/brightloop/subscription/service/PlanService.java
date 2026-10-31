package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.PlanRequest;
import com.brightloop.subscription.web.dto.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface PlanService {
    PlanResponse createPlan(PlanRequest request);
    PlanResponse updatePlan(Long id, PlanRequest request);
    PlanResponse getPlanById(Long id);
    PlanResponse getPlanByCode(String code);
    Page<PlanResponse> getAllPlans(Pageable pageable, Boolean activeOnly, Long productId);
    List<PlanResponse> getActivePlans();
    LocalDate calculateNextBillingDate(Long planId, LocalDate fromDate);
}

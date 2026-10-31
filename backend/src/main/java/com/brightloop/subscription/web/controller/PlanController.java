package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.PlanService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.PlanRequest;
import com.brightloop.subscription.web.dto.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Subscription Plans", description = "Endpoints for defining subscription plans, pricing intervals, and policies")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "List all plans with optional active and product filters (Paginated)")
    public ResponseEntity<ApiResponse<Page<PlanResponse>>> getAllPlans(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<PlanResponse> plans = planService.getAllPlans(pageable, activeOnly, productId);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active subscription plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getActivePlans() {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlans()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlanById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new subscription plan (Admin Only)")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(@Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing subscription plan (Admin Only)")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", response));
    }

    @GetMapping("/{id}/next-billing-date")
    @Operation(summary = "Calculate next billing date from a reference start date according to plan frequency")
    public ResponseEntity<ApiResponse<LocalDate>> getNextBillingDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        LocalDate nextDate = planService.calculateNextBillingDate(id, fromDate);
        return ResponseEntity.ok(ApiResponse.success(nextDate));
    }
}

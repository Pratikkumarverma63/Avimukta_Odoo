package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.DashboardService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.DashboardMetricsResponse;
import com.brightloop.subscription.web.dto.RevenueTrendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Analytics & Finance Dashboard", description = "Endpoints for executive and financial metrics, revenue trends, and subscription analytics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get aggregated analytics and finance metrics")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId) {
        DashboardMetricsResponse metrics = dashboardService.getMetrics(startDate, endDate, customerId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", metrics));
    }

    @GetMapping("/revenue-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get 6-month historical revenue trend")
    public ResponseEntity<ApiResponse<RevenueTrendResponse>> getRevenueTrend() {
        RevenueTrendResponse trend = dashboardService.getRevenueTrend();
        return ResponseEntity.ok(ApiResponse.success("Revenue trend retrieved successfully", trend));
    }
}

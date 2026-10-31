package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.BillingEngineService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.BillingExecutionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Automated Billing Engine", description = "Endpoints for triggering and inspecting recurring subscription billing runs")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingEngineService billingEngineService;

    public BillingController(BillingEngineService billingEngineService) {
        this.billingEngineService = billingEngineService;
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger the automated recurring billing run (Admin Only)")
    public ResponseEntity<ApiResponse<BillingExecutionSummaryResponse>> triggerBillingRun(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        BillingExecutionSummaryResponse summary = billingEngineService.runBillingJob(date);
        return ResponseEntity.ok(ApiResponse.success("Billing run completed", summary));
    }
}

package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.domain.entity.ProrationRecord;
import com.brightloop.subscription.service.ProrationService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.ProrationCalculationRequest;
import com.brightloop.subscription.web.dto.ProrationCalculationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions/{id}/prorate")
@Tag(name = "Proration Engine", description = "Unique Hackathon Feature: Mid-cycle subscription upgrade/downgrade pro-rata credit calculation")
@SecurityRequirement(name = "bearerAuth")
public class ProrationController {

    private final ProrationService prorationService;

    public ProrationController(ProrationService prorationService) {
        this.prorationService = prorationService;
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Calculate exact daily pro-rata credit and charge for mid-cycle plan switch")
    public ResponseEntity<ApiResponse<ProrationCalculationResponse>> calculate(
            @PathVariable Long id,
            @Valid @RequestBody ProrationCalculationRequest request) {
        ProrationCalculationResponse response = prorationService.calculateProration(id, request);
        return ResponseEntity.ok(ApiResponse.success("Proration calculation successful", response));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Apply mid-cycle plan switch, update subscription, and record proration ledger entry")
    public ResponseEntity<ApiResponse<ProrationCalculationResponse>> apply(
            @PathVariable Long id,
            @Valid @RequestBody ProrationCalculationRequest request) {
        ProrationCalculationResponse response = prorationService.applyProration(id, request);
        return ResponseEntity.ok(ApiResponse.success("Proration successfully applied to subscription", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get historical proration ledger audit records for this subscription")
    public ResponseEntity<ApiResponse<List<ProrationRecord>>> getHistory(@PathVariable Long id) {
        List<ProrationRecord> history = prorationService.getProrationsForSubscription(id);
        return ResponseEntity.ok(ApiResponse.success("Proration history retrieved", history));
    }
}

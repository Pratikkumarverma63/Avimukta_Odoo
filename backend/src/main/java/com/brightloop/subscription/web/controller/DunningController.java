package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.domain.entity.DunningCase;
import com.brightloop.subscription.service.DunningService;
import com.brightloop.subscription.web.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dunning")
@Tag(name = "Smart Dunning & Revenue Recovery", description = "Unique Hackathon Feature: Automated 4-tier dunning engine with late fees, grace periods, and service suspension triggers")
@SecurityRequirement(name = "bearerAuth")
public class DunningController {

    private final DunningService dunningService;

    public DunningController(DunningService dunningService) {
        this.dunningService = dunningService;
    }

    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all active dunning and revenue recovery cases")
    public ResponseEntity<ApiResponse<List<DunningCase>>> getCases() {
        List<DunningCase> cases = dunningService.getAllActiveCases();
        return ResponseEntity.ok(ApiResponse.success("Active dunning cases retrieved", cases));
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Execute automated dunning recovery job across all overdue invoices")
    public ResponseEntity<ApiResponse<String>> processDunning() {
        int processed = dunningService.processOverdueInvoices();
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Dunning processor executed. %d overdue invoice case(s) updated with late fee compounding.", processed),
                null));
    }
}

package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.TaxService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.TaxRuleRequest;
import com.brightloop.subscription.web.dto.TaxRuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxes")
@Tag(name = "Tax Management", description = "Endpoints for configuring and applying centralized tax rules")
@SecurityRequirement(name = "bearerAuth")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    @GetMapping
    @Operation(summary = "List all tax rules (Admin and Staff)")
    public ResponseEntity<ApiResponse<List<TaxRuleResponse>>> getAllTaxRules() {
        return ResponseEntity.ok(ApiResponse.success(taxService.getAllTaxRules()));
    }

    @GetMapping("/active")
    @Operation(summary = "List active tax rules")
    public ResponseEntity<ApiResponse<List<TaxRuleResponse>>> getActiveTaxRules() {
        return ResponseEntity.ok(ApiResponse.success(taxService.getActiveTaxRules()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax rule by ID")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> getTaxRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taxService.getTaxRuleById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tax rule (Admin Only)")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> createTaxRule(@Valid @RequestBody TaxRuleRequest request) {
        TaxRuleResponse response = taxService.createTaxRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax rule created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing tax rule (Admin Only)")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> updateTaxRule(
            @PathVariable Long id,
            @Valid @RequestBody TaxRuleRequest request) {
        TaxRuleResponse response = taxService.updateTaxRule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tax rule updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a tax rule (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteTaxRule(@PathVariable Long id) {
        taxService.deleteTaxRule(id);
        return ResponseEntity.ok(ApiResponse.success("Tax rule deactivated successfully", null));
    }
}

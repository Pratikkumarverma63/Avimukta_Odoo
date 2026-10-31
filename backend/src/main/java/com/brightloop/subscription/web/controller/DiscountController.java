package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.DiscountService;
import com.brightloop.subscription.web.dto.*;
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
@RequestMapping("/api/v1/discounts")
@Tag(name = "Discount Management", description = "Endpoints for configuring promotional discount rules, usage limits, and eligibility validation")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    @Operation(summary = "List all discount rules (Admin and Staff)")
    public ResponseEntity<ApiResponse<List<DiscountRuleResponse>>> getAllDiscountRules() {
        return ResponseEntity.ok(ApiResponse.success(discountService.getAllDiscountRules()));
    }

    @GetMapping("/active")
    @Operation(summary = "List active discount rules")
    public ResponseEntity<ApiResponse<List<DiscountRuleResponse>>> getActiveDiscountRules() {
        return ResponseEntity.ok(ApiResponse.success(discountService.getActiveDiscountRules()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get discount rule by ID")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> getDiscountRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(discountService.getDiscountRuleById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new discount rule (Admin Only)")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> createDiscountRule(
            @Valid @RequestBody DiscountRuleRequest request) {
        DiscountRuleResponse response = discountService.createDiscountRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discount rule created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing discount rule (Admin Only)")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> updateDiscountRule(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRuleRequest request) {
        DiscountRuleResponse response = discountService.updateDiscountRule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Discount rule updated successfully", response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate discount code eligibility against purchase amount and quantity")
    public ResponseEntity<ApiResponse<DiscountValidationResponse>> validateDiscount(
            @Valid @RequestBody DiscountValidationRequest request) {
        DiscountValidationResponse response = discountService.validateDiscount(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a discount rule (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteDiscountRule(@PathVariable Long id) {
        discountService.deleteDiscountRule(id);
        return ResponseEntity.ok(ApiResponse.success("Discount rule deactivated successfully", null));
    }
}

package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.service.SubscriptionService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.SubscriptionRequest;
import com.brightloop.subscription.web.dto.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Endpoints for managing customer subscription lifecycles, states, and operations")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "List subscriptions with status and customer filter (Paginated, isolated for Customers)")
    public ResponseEntity<ApiResponse<Page<SubscriptionResponse>>> getAllSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions(pageable, status, customerId);
        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription details by ID (Isolated for Customers)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getSubscriptionById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create subscription directly (Admin and Staff)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created successfully", response));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause an active subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> pauseSubscription(@PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.pauseSubscription(id);
        return ResponseEntity.ok(ApiResponse.success("Subscription paused successfully", response));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a paused subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resumeSubscription(@PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.resumeSubscription(id);
        return ResponseEntity.ok(ApiResponse.success("Subscription resumed successfully", response));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Early closure of subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> closeSubscription(@PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.closeSubscription(id);
        return ResponseEntity.ok(ApiResponse.success("Subscription closed successfully", response));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew subscription for an additional term")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> renewSubscription(@PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.renewSubscription(id);
        return ResponseEntity.ok(ApiResponse.success("Subscription renewed successfully", response));
    }
}

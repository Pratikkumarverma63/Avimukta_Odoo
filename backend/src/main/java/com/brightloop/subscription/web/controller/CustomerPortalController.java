package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.CustomerPortalService;
import com.brightloop.subscription.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer Portal", description = "Endpoints dedicated for customer self-service, personal dashboard, profile and payments")
@SecurityRequirement(name = "bearerAuth")
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;

    public CustomerPortalController(CustomerPortalService customerPortalService) {
        this.customerPortalService = customerPortalService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current customer portal dashboard overview")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getDashboard() {
        CustomerDashboardResponse dashboard = customerPortalService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update customer company profile details")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> updateProfile(
            @Valid @RequestBody CustomerProfileRequest request) {
        CustomerDashboardResponse updated = customerPortalService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Change customer login password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        customerPortalService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/cancel-subscription")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Request cancellation of an active subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @RequestParam Long subscriptionId,
            @RequestParam(required = false) String reason) {
        SubscriptionResponse response = customerPortalService.requestCancellation(subscriptionId, reason);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancellation processed", response));
    }

    @PostMapping("/subscribe")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Self-service subscription to a plan")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @RequestParam Long planId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") int quantity) {
        SubscriptionResponse response = customerPortalService.subscribeToPlan(planId, variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Subscribed successfully to plan", response));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer in-app notifications (Paginated)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> notifications = customerPortalService.getNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PutMapping("/notifications/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable Long id) {
        customerPortalService.markNotificationRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/notifications/read-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead() {
        customerPortalService.markAllNotificationsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}

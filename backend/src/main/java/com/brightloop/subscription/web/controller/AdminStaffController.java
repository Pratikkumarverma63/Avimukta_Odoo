package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.AuthService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.StaffCreateRequest;
import com.brightloop.subscription.web.dto.UserProfileResponse;
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
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Staff Management", description = "Endpoints reserved for administrators to create and manage internal staff accounts")
@SecurityRequirement(name = "bearerAuth")
public class AdminStaffController {

    private final AuthService authService;

    public AdminStaffController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Create a new internal staff account (Admin Only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createStaff(@Valid @RequestBody StaffCreateRequest request) {
        UserProfileResponse response = authService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff account created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all internal staff accounts (Admin Only)")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllStaff() {
        List<UserProfileResponse> response = authService.getAllStaff();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

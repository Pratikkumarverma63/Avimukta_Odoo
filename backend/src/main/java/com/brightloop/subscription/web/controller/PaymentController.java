package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.PaymentService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.PaymentRequest;
import com.brightloop.subscription.web.dto.PaymentResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments & Reconciliation", description = "Endpoints for recording payments across payment rails, partial payment reconciliations, and history")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Record a payment against an invoice (Admin and Staff)")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all payments with optional customer filter (Paginated, isolated for Customers)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable, customerId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment receipt by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Get all payments associated with an invoice")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByInvoiceId(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByInvoiceId(invoiceId)));
    }
}

package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.domain.enums.QuotationStatus;
import com.brightloop.subscription.service.QuotationService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.QuotationRequest;
import com.brightloop.subscription.web.dto.QuotationResponse;
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
@RequestMapping("/api/v1/quotations")
@Tag(name = "Quotations", description = "Endpoints for creating, managing, sending, and converting sales quotations")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    @Operation(summary = "List quotations with status and customer filter (Paginated)")
    public ResponseEntity<ApiResponse<Page<QuotationResponse>>> getAllQuotations(
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuotationResponse> quotations = quotationService.getAllQuotations(pageable, status, customerId);
        return ResponseEntity.ok(ApiResponse.success(quotations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get quotation by ID with line items and pricing breakdown")
    public ResponseEntity<ApiResponse<QuotationResponse>> getQuotationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quotationService.getQuotationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create custom quotation (Admin and Staff)")
    public ResponseEntity<ApiResponse<QuotationResponse>> createQuotation(@Valid @RequestBody QuotationRequest request) {
        QuotationResponse response = quotationService.createQuotation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation created successfully", response));
    }

    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create quotation from a reusable quotation template (Admin and Staff)")
    public ResponseEntity<ApiResponse<QuotationResponse>> createFromTemplate(
            @PathVariable Long templateId,
            @RequestParam Long customerId,
            @RequestParam(required = false, defaultValue = "NET_30") String paymentTerms) {
        QuotationResponse response = quotationService.createFromTemplate(templateId, customerId, paymentTerms);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation created from template", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update a draft quotation (Admin and Staff)")
    public ResponseEntity<ApiResponse<QuotationResponse>> updateQuotation(
            @PathVariable Long id,
            @Valid @RequestBody QuotationRequest request) {
        QuotationResponse response = quotationService.updateQuotation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Quotation updated successfully", response));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Send quotation to customer (Transitions DRAFT -> QUOTATION)")
    public ResponseEntity<ApiResponse<QuotationResponse>> sendQuotation(@PathVariable Long id) {
        QuotationResponse response = quotationService.sendQuotation(id);
        return ResponseEntity.ok(ApiResponse.success("Quotation dispatched to customer", response));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm quotation and convert it into an Active Subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> confirmQuotation(@PathVariable Long id) {
        SubscriptionResponse response = quotationService.confirmQuotation(id);
        return ResponseEntity.ok(ApiResponse.success("Quotation confirmed and converted to subscription", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a quotation")
    public ResponseEntity<ApiResponse<QuotationResponse>> cancelQuotation(@PathVariable Long id) {
        QuotationResponse response = quotationService.cancelQuotation(id);
        return ResponseEntity.ok(ApiResponse.success("Quotation cancelled", response));
    }
}

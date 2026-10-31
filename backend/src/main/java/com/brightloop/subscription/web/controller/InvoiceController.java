package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.service.InvoiceService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Endpoints for managing customer invoices, printing, and tracking status")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @Operation(summary = "List invoices with status and customer filter (Paginated, isolated for Customers)")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Boolean overdueOnly,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InvoiceResponse> invoices = invoiceService.getAllInvoices(pageable, status, customerId, overdueOnly);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice details by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceById(id)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Confirm draft invoice (Admin and Staff)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> confirmInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice confirmed", invoiceService.confirmInvoice(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Cancel invoice (Admin and Staff)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled", invoiceService.cancelInvoice(id)));
    }

    @GetMapping(value = {"/{id}/printable", "/{id}/print"}, produces = {MediaType.TEXT_HTML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Operation(summary = "Generate professional printable invoice layout / PDF view")
    public ResponseEntity<String> getPrintableInvoice(@PathVariable Long id) {
        String html = invoiceService.generatePrintableInvoiceHtml(id);
        return ResponseEntity.ok(html);
    }
}

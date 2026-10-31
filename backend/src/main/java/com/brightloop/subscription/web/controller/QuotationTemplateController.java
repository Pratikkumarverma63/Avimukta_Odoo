package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.QuotationTemplateService;
import com.brightloop.subscription.web.dto.ApiResponse;
import com.brightloop.subscription.web.dto.QuotationTemplateRequest;
import com.brightloop.subscription.web.dto.QuotationTemplateResponse;
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
@RequestMapping("/api/v1/quotation-templates")
@Tag(name = "Quotation Templates", description = "Endpoints for managing reusable quotation templates")
@SecurityRequirement(name = "bearerAuth")
public class QuotationTemplateController {

    private final QuotationTemplateService templateService;

    public QuotationTemplateController(QuotationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List all quotation templates (Admin and Staff)")
    public ResponseEntity<ApiResponse<List<QuotationTemplateResponse>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllTemplates()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get quotation template by ID")
    public ResponseEntity<ApiResponse<QuotationTemplateResponse>> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplateById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new quotation template (Admin Only)")
    public ResponseEntity<ApiResponse<QuotationTemplateResponse>> createTemplate(
            @Valid @RequestBody QuotationTemplateRequest request) {
        QuotationTemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a quotation template (Admin Only)")
    public ResponseEntity<ApiResponse<QuotationTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody QuotationTemplateRequest request) {
        QuotationTemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a quotation template (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deactivated successfully", null));
    }
}

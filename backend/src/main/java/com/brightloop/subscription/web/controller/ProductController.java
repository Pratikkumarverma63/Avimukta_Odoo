package com.brightloop.subscription.web.controller;

import com.brightloop.subscription.service.ProductService;
import com.brightloop.subscription.web.dto.*;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products & Variants", description = "Endpoints for managing products, configurable pricing, and variant attributes")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List all products with search and active filter (Paginated)")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> products = productService.getAllProducts(pageable, activeOnly, search);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getActiveProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getActiveProducts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID with all active variants")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (Admin Only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing product (Admin Only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a configurable variant to a product (Admin Only)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.addVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product variant added successfully", response));
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product variant (Admin Only)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.updateVariant(variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Product variant updated successfully", response));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a product variant (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant deactivated successfully", null));
    }

    @GetMapping("/{productId}/effective-price")
    @Operation(summary = "Calculate combined price for product and optional variant")
    public ResponseEntity<ApiResponse<BigDecimal>> getEffectivePrice(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        BigDecimal price = productService.calculateEffectivePrice(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success(price));
    }
}

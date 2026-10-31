package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse getProductByCode(String code);
    Page<ProductResponse> getAllProducts(Pageable pageable, Boolean activeOnly, String search);
    List<ProductResponse> getActiveProducts();
    
    ProductVariantResponse addVariant(Long productId, ProductVariantRequest request);
    ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request);
    void deleteVariant(Long variantId);
    BigDecimal calculateEffectivePrice(Long productId, Long variantId);
}

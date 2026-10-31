package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.Product;
import com.brightloop.subscription.domain.entity.ProductVariant;
import com.brightloop.subscription.repository.ProductRepository;
import com.brightloop.subscription.repository.ProductVariantRepository;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.ProductService;
import com.brightloop.subscription.web.dto.*;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditLogService auditLogService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Product with code '" + request.getCode() + "' already exists.");
        }

        Product product = Product.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .productType(request.getProductType())
                .salesPrice(request.getSalesPrice())
                .costPrice(request.getCostPrice())
                .description(request.getDescription())
                .active(request.isActive())
                .build();

        product = productRepository.save(product);
        auditLogService.logAction("PRODUCT_CREATED", "Product", product.getId(), null, product.getCode());

        return mapToProductResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getCode().equalsIgnoreCase(request.getCode().trim()) &&
                productRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Product with code '" + request.getCode() + "' already exists.");
        }

        product.setCode(request.getCode().trim().toUpperCase());
        product.setName(request.getName().trim());
        product.setProductType(request.getProductType());
        product.setSalesPrice(request.getSalesPrice());
        product.setCostPrice(request.getCostPrice());
        product.setDescription(request.getDescription());
        product.setActive(request.isActive());

        product = productRepository.save(product);
        auditLogService.logAction("PRODUCT_UPDATED", "Product", product.getId(), null, product.getCode());

        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByCode(String code) {
        Product product = productRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with code: " + code));
        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, Boolean activeOnly, String search) {
        Page<Product> page;
        if (StringUtils.hasText(search)) {
            page = productRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            page = productRepository.findByActive(true, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return page.map(this::mapToProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActive(true, Pageable.unpaged())
                .getContent().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse addVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        String sku = request.getSku();
        if (!StringUtils.hasText(sku)) {
            sku = product.getCode() + "-" + request.getAttributeName().toUpperCase() + "-" + request.getVariantValue().toUpperCase();
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .attributeName(request.getAttributeName().trim())
                .variantValue(request.getVariantValue().trim())
                .priceAdjustment(request.getPriceAdjustment())
                .sku(sku)
                .active(request.isActive())
                .build();

        variant = productVariantRepository.save(variant);
        auditLogService.logAction("PRODUCT_VARIANT_ADDED", "ProductVariant", variant.getId(), null, variant.getSku());

        return mapToVariantResponse(variant, product.getSalesPrice());
    }

    @Override
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));

        variant.setAttributeName(request.getAttributeName().trim());
        variant.setVariantValue(request.getVariantValue().trim());
        variant.setPriceAdjustment(request.getPriceAdjustment());
        if (StringUtils.hasText(request.getSku())) {
            variant.setSku(request.getSku().trim());
        }
        variant.setActive(request.isActive());

        variant = productVariantRepository.save(variant);
        return mapToVariantResponse(variant, variant.getProduct().getSalesPrice());
    }

    @Override
    public void deleteVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
        variant.setActive(false);
        productVariantRepository.save(variant);
        auditLogService.logAction("PRODUCT_VARIANT_DEACTIVATED", "ProductVariant", variant.getId(), null, variant.getSku());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateEffectivePrice(Long productId, Long variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        BigDecimal price = product.getSalesPrice();
        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
            if (!variant.getProduct().getId().equals(productId)) {
                throw new BadRequestException("Variant does not belong to specified product.");
            }
            price = price.add(variant.getPriceAdjustment());
        }
        return price;
    }

    private ProductResponse mapToProductResponse(Product product) {
        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(v -> mapToVariantResponse(v, product.getSalesPrice()))
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .productType(product.getProductType())
                .salesPrice(product.getSalesPrice())
                .costPrice(product.getCostPrice())
                .description(product.getDescription())
                .active(product.isActive())
                .variants(variantResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductVariantResponse mapToVariantResponse(ProductVariant variant, BigDecimal basePrice) {
        BigDecimal effective = basePrice.add(variant.getPriceAdjustment() != null ? variant.getPriceAdjustment() : BigDecimal.ZERO);
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .attributeName(variant.getAttributeName())
                .variantValue(variant.getVariantValue())
                .priceAdjustment(variant.getPriceAdjustment())
                .effectivePrice(effective)
                .sku(variant.getSku())
                .active(variant.isActive())
                .build();
    }
}

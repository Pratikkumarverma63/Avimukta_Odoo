package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String code;
    private String name;
    private ProductType productType;
    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private String description;
    private boolean active;
    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

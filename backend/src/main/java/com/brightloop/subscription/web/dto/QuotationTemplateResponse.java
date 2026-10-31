package com.brightloop.subscription.web.dto;

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
public class QuotationTemplateResponse {
    private Long id;
    private String name;
    private Long defaultPlanId;
    private String defaultPlanName;
    private Integer validityDays;
    private boolean active;
    @Builder.Default
    private List<TemplateItemResponse> items = new ArrayList<>();
    private OffsetDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateItemResponse {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private Long variantId;
        private String variantName;
        private Integer defaultQuantity;
        private BigDecimal unitPrice;
    }
}

package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private Long defaultPlanId;

    @Min(value = 1, message = "Validity days must be at least 1")
    @Builder.Default
    private Integer validityDays = 30;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private List<TemplateItemRequest> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateItemRequest {
        private Long productId;
        private Long variantId;
        private Integer defaultQuantity;
        private BigDecimal unitPrice;
    }
}

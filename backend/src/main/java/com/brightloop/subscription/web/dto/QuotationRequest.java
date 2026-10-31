package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Builder.Default
    private String paymentTerms = "NET_30";

    private String notes;

    @NotEmpty(message = "At least one item is required in the quotation")
    @Builder.Default
    private List<QuotationItemRequest> items = new ArrayList<>();
}

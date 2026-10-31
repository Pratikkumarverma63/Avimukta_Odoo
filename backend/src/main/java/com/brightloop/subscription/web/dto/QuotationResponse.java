package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.QuotationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponse {
    private Long id;
    private String quotationNumber;
    private Long customerId;
    private String customerCode;
    private String customerCompanyName;
    private String customerEmail;
    private Long planId;
    private String planName;
    private QuotationStatus status;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String paymentTerms;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String notes;
    private Long createdById;
    private String createdByName;
    @Builder.Default
    private List<QuotationItemResponse> items = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

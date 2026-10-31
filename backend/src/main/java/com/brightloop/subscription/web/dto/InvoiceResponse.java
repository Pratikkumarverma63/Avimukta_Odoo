package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.InvoiceStatus;
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
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long subscriptionId;
    private String subscriptionNumber;
    private Long customerId;
    private String customerCode;
    private String customerCompanyName;
    private String customerContactName;
    private String customerEmail;
    private String customerPhone;
    private String customerBillingAddress;
    private String customerTaxId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private boolean overdue;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal outstandingAmount;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
    @Builder.Default
    private List<InvoiceItemResponse> items = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

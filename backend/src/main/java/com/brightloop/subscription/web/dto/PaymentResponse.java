package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.PaymentMethod;
import com.brightloop.subscription.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private OffsetDateTime paymentDate;
    private String transactionReference;
    private String notes;
    private PaymentStatus status;
    private Long createdById;
    private String createdByName;
    private OffsetDateTime createdAt;
}

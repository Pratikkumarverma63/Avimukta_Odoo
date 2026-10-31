package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.PaymentRequest;
import com.brightloop.subscription.web.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    PaymentResponse recordPayment(PaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId);
    Page<PaymentResponse> getAllPayments(Pageable pageable, Long customerId);
}

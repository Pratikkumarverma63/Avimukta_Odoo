package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.enums.QuotationStatus;
import com.brightloop.subscription.web.dto.QuotationRequest;
import com.brightloop.subscription.web.dto.QuotationResponse;
import com.brightloop.subscription.web.dto.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuotationService {
    QuotationResponse createQuotation(QuotationRequest request);
    QuotationResponse createFromTemplate(Long templateId, Long customerId, String paymentTerms);
    QuotationResponse updateQuotation(Long id, QuotationRequest request);
    QuotationResponse getQuotationById(Long id);
    Page<QuotationResponse> getAllQuotations(Pageable pageable, QuotationStatus status, Long customerId);
    QuotationResponse sendQuotation(Long id);
    SubscriptionResponse confirmQuotation(Long id);
    QuotationResponse cancelQuotation(Long id);
}

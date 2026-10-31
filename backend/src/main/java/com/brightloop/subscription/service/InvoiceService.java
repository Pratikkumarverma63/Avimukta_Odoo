package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.enums.InvoiceStatus;
import com.brightloop.subscription.web.dto.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    InvoiceResponse getInvoiceById(Long id);
    Page<InvoiceResponse> getAllInvoices(Pageable pageable, InvoiceStatus status, Long customerId, Boolean overdueOnly);
    InvoiceResponse confirmInvoice(Long id);
    InvoiceResponse cancelInvoice(Long id);
    String generatePrintableInvoiceHtml(Long id);
}

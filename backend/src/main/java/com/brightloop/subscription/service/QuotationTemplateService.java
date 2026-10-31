package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.QuotationTemplateRequest;
import com.brightloop.subscription.web.dto.QuotationTemplateResponse;

import java.util.List;

public interface QuotationTemplateService {
    QuotationTemplateResponse createTemplate(QuotationTemplateRequest request);
    QuotationTemplateResponse updateTemplate(Long id, QuotationTemplateRequest request);
    QuotationTemplateResponse getTemplateById(Long id);
    List<QuotationTemplateResponse> getAllTemplates();
    void deleteTemplate(Long id);
}

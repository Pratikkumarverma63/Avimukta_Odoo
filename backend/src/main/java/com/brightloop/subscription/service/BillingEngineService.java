package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.BillingExecutionSummaryResponse;

import java.time.LocalDate;

public interface BillingEngineService {
    BillingExecutionSummaryResponse runBillingJob(LocalDate asOfDate);
}

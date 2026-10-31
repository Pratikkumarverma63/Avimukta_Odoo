package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.entity.ProrationRecord;
import com.brightloop.subscription.web.dto.ProrationCalculationRequest;
import com.brightloop.subscription.web.dto.ProrationCalculationResponse;

import java.util.List;

public interface ProrationService {
    ProrationCalculationResponse calculateProration(Long subscriptionId, ProrationCalculationRequest request);
    ProrationCalculationResponse applyProration(Long subscriptionId, ProrationCalculationRequest request);
    List<ProrationRecord> getProrationsForSubscription(Long subscriptionId);
}

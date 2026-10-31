package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.enums.SubscriptionStatus;
import com.brightloop.subscription.web.dto.SubscriptionRequest;
import com.brightloop.subscription.web.dto.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(SubscriptionRequest request);
    SubscriptionResponse getSubscriptionById(Long id);
    Page<SubscriptionResponse> getAllSubscriptions(Pageable pageable, SubscriptionStatus status, Long customerId);
    SubscriptionResponse pauseSubscription(Long id);
    SubscriptionResponse resumeSubscription(Long id);
    SubscriptionResponse closeSubscription(Long id);
    SubscriptionResponse renewSubscription(Long id);
    void autoCloseExpiredSubscriptions();
}

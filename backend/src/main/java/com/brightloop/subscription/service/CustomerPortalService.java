package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerPortalService {
    CustomerDashboardResponse getDashboard();
    Page<NotificationResponse> getNotifications(Pageable pageable);
    void markNotificationRead(Long id);
    void markAllNotificationsRead();
    SubscriptionResponse subscribeToPlan(Long planId, Long variantId, int quantity);
    CustomerDashboardResponse updateProfile(CustomerProfileRequest request);
    void changePassword(ChangePasswordRequest request);
    SubscriptionResponse requestCancellation(Long subscriptionId, String reason);
}

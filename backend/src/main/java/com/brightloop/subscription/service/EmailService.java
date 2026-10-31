package com.brightloop.subscription.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendNotificationEmail(String toEmail, String subject, String body);
}

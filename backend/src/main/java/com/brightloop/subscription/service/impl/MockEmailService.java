package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);
    private final ConcurrentHashMap<String, String> lastResetTokens = new ConcurrentHashMap<>();

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        lastResetTokens.put(toEmail, resetToken);
        log.info("==================================================");
        log.info("PASSWORD RESET EMAIL DISPATCHED");
        log.info("Recipient: {}", toEmail);
        log.info("Reset Token: {}", resetToken);
        log.info("Reset URL: https://brightloop.com/reset-password?token={}", resetToken);
        log.info("==================================================");
    }

    @Override
    public void sendNotificationEmail(String toEmail, String subject, String body) {
        log.info("EMAIL NOTIFICATION -> To: {}, Subject: {}", toEmail, subject);
    }

    public String getLastResetTokenForEmail(String email) {
        return lastResetTokens.get(email);
    }
}

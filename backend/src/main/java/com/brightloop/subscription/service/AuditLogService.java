package com.brightloop.subscription.service;

import com.brightloop.subscription.domain.entity.AuditLog;
import com.brightloop.subscription.domain.entity.User;
import com.brightloop.subscription.repository.AuditLogRepository;
import com.brightloop.subscription.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, Long entityId, String oldValue, String newValue) {
        User currentUser = SecurityUtils.getCurrentUser().orElse(null);

        AuditLog log = AuditLog.builder()
                .user(currentUser)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);
    }
}

package com.brightloop.subscription.web.dto;

import com.brightloop.subscription.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private boolean read;
    private String referenceType;
    private Long referenceId;
    private OffsetDateTime createdAt;
}

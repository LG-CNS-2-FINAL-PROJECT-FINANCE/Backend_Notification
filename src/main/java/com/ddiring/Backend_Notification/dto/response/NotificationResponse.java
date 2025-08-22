package com.ddiring.Backend_Notification.dto.response;

import com.ddiring.Backend_Notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Integer notificationSeq;
    private NotificationType notificationType;
    private String message;
    private LocalDateTime createdAt;
}

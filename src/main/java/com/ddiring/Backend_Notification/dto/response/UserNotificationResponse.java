package com.ddiring.Backend_Notification.dto.response;

import com.ddiring.Backend_Notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationResponse {
    private Integer userNotificationSeq;
    private Integer userSeq;
    private NotificationStatus notificationStatus;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private NotificationResponse notification;
}

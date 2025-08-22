package com.ddiring.Backend_Notification.dto.request;

import com.ddiring.Backend_Notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    private Integer userSeq;
    private NotificationType notificationType;
    private String message;
}

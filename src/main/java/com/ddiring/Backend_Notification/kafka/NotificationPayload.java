package com.ddiring.Backend_Notification.kafka;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private List<Integer> userSeqs;        // 수신자
    private String notificationType;      // 알림 타입
    private String message;               // 알림 메시지
}
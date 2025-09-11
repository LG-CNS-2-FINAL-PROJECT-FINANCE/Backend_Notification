package com.ddiring.Backend_Notification.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private List<String> userSeq;          // 수신자
    private String notificationType;       // 알림 타입
    private String title;                  // 제목
    private String message;                // 알림 메시지

    // 새 title/message로 새로운 객체 생성
    public NotificationPayload withTitleAndMessage(String title, String message) {
        return NotificationPayload.builder()
                .userSeq(this.userSeq)
                .notificationType(this.notificationType)
                .title(title)
                .message(message)
                .build();
    }
}

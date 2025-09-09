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
    private List<String> userSeq;         //수신자
    private String notificationType;       //알림 타입
    private String title;                  //제목
    private String message;                //알림 메시지
}
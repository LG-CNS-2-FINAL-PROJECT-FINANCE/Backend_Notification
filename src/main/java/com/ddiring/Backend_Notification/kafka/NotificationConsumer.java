package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consume(String message) {
        try {
            EventEnvelope<NotificationPayload> envelope =
                    objectMapper.readValue(message, new TypeReference<>() {});

            // 원본 메시지 로그
            System.out.println("📩 수신한 원본 메시지: " + message);

            // envelope 전체 확인
            System.out.println("📝 역직렬화된 EventEnvelope: " + envelope);

            // 개별 필드 확인
            System.out.println("➡️ Payload: " + envelope.getPayload());

            // 실제 서비스 호출
            notificationService.handleNotificationEvent(envelope.getPayload());

            System.out.println("✅ Kafka 이벤트 처리 완료");
        } catch (Exception e) {
            System.err.println("⚠️ Kafka 역직렬화 실패: " + e.getMessage());
        }
    }

}
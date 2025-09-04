package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private static final String MAIN_TOPIC = "notification";

    @KafkaListener(topics = MAIN_TOPIC, groupId = "notification-group")
    public void consume(String message) {
        try {
            // JSON String → Map → DTO
            Map<String, Object> messageMap = objectMapper.readValue(message, new TypeReference<>() {});
            EventEnvelope<NotificationPayload> envelope =
                    objectMapper.convertValue(messageMap, new TypeReference<EventEnvelope<NotificationPayload>>() {});

            // NotificationService 호출
            notificationService.handleNotificationEvent(envelope);

            System.out.println("✅ Kafka 이벤트 처리 완료: " + envelope);
        } catch (Exception e) {
            System.err.println("⚠️ Kafka 역직렬화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
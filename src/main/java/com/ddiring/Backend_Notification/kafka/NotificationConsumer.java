package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String MAIN_TOPIC = "notification";
    private static final String DLQ_TOPIC  = "notification-dlq";

    @KafkaListener(topics = MAIN_TOPIC, groupId = "notification-group")
    public void consume(String message) {
        try {
            EventEnvelope<NotificationPayload> envelope =
                    objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<EventEnvelope<NotificationPayload>>() {});

            notificationService.handleNotificationEvent(envelope);
        } catch (Exception e) {
            // Consumer 처리 실패 → DLQ
            kafkaTemplate.send(DLQ_TOPIC, message);
            System.err.println("[DLQ] moved due to Consumer error: " + e.getMessage());
        }
    }
}

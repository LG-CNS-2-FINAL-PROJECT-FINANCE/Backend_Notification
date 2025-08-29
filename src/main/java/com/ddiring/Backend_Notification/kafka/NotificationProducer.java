package com.ddiring.Backend_Notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "notification";
    private static final String DLQ_TOPIC = "notification-dlq";

    public void sendNotification(NotificationPayload payload) {
        try {
            EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .payload(payload)
                    .build();

            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, json);
        } catch (Exception e) {
            System.err.println("[Producer] Kafka 직렬화 실패, DLQ로 이동: " + e.getMessage());
            sendToDLQ(payload);
        }
    }

    private void sendToDLQ(NotificationPayload payload) {
        try {
            EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .payload(payload)
                    .build();
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(DLQ_TOPIC, json);
        } catch (Exception e) {
            System.err.println("[DLQ] 전송 실패: " + e.getMessage());
        }
    }
}

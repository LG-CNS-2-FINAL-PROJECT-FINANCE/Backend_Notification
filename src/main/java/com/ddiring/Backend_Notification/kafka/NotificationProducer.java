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

    private static final String TOPIC = "notification-topic";

    public void sendNotification(NotificationPayload payload) {
        try {
            EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .payload(payload)
                    .build();

            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, json);
            System.out.println("Kafka 메시지 전송 완료: " + json);
        } catch (Exception e) {
            throw new RuntimeException("Kafka 직렬화 실패", e);
        }
    }
}
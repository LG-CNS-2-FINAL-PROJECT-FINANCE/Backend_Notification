package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
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
                    objectMapper.readValue(message, new TypeReference<EventEnvelope<NotificationPayload>>() {});
            log.info("🎯 [Consumer 수신] eventId={}, payload={}", envelope.getEventId(), envelope.getPayload());

            notificationService.handleNotificationEvent(envelope);
        } catch (Exception e) {
            log.error("❌ [Consumer 처리 실패] message={}, error={}", message, e.getMessage(), e);
            kafkaTemplate.send(DLQ_TOPIC, message);
            log.warn("[DLQ 이동] message sent to DLQ");
        }
    }

}

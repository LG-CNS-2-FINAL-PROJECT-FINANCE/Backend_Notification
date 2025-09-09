package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification", groupId = "notification")
    public void consume(String message) {
        try {
            EventEnvelope<NotificationPayload> envelope = objectMapper
                    .readValue(message, objectMapper.getTypeFactory()
                            .constructParametricType(EventEnvelope.class, NotificationPayload.class));

            if (envelope == null || envelope.getPayload() == null) {
                log.warn("잘못된 Kafka 이벤트 수신: {}", envelope);
                return;
            }

            notificationService.handleNotificationEvent(envelope);

        } catch (Exception e) {
            log.error("Kafka 이벤트 처리 실패: {}", message, e);
        }
    }
}
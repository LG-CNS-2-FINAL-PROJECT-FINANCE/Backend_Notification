package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final String MAIN_TOPIC = "notification";

    @KafkaListener(topics = MAIN_TOPIC, groupId = "notification-group")
    public void consume(String message) {
        try {
            Map<String, Object> messageMap = objectMapper.readValue(message, new TypeReference<>() {});

            NotificationEvent event = objectMapper.convertValue(messageMap, NotificationEvent.class);
            notificationService.handleNotificationEvent(event);
        } catch (Exception e) {
            log.error("Consumer 처리 실패: message={}, error={}", message, e.getMessage(), e);
        }
    }

}
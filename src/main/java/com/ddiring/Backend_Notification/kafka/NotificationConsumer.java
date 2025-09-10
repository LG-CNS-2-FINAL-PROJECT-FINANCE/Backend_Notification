package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_TOPIC = "notification";
    private static final String SMARTCONTRACT_TOPIC = "INVESTMENT";

    private static final Set<String> ALLOWED_SMARTCONTRACT_EVENTS = Set.of(
            "INVESTMENT.SUCCEEDED",
            "INVESTMENT.FAILED",
            "TRADE.SUCCEEDED",
            "TRADE.FAILED"
    );

    @KafkaListener(topics = NOTIFICATION_TOPIC, groupId = "BackendNotification")
    public void consume(String message, Acknowledgment ack) {
        try {
            EventEnvelope<NotificationPayload> envelope =
                    objectMapper.readValue(message, new TypeReference<EventEnvelope<NotificationPayload>>() {});

            if (envelope == null || envelope.getPayload() == null) {
                log.warn("잘못된 Kafka 알림 이벤트 수신: {}", message);
                ack.acknowledge();
                return;
            }

            notificationService.handleNotificationEvent(envelope);
            ack.acknowledge();
            log.info("Kafka 알림 이벤트 처리 완료: eventId={}", envelope.getEventId());
        } catch (Exception e) {
            log.error("Kafka 알림 이벤트 처리 실패: {}", message, e);
        }
    }

    @KafkaListener(topics = SMARTCONTRACT_TOPIC, groupId = "BackendNotification")
    public void smartContractConsume(String message, Acknowledgment ack) {
        try {
            EventEnvelope<SmartContractPayload> envelope =
                    objectMapper.readValue(message, new TypeReference<EventEnvelope<SmartContractPayload>>() {});

            if (envelope == null || envelope.getPayload() == null) {
                log.warn("잘못된 Kafka 스마트컨트랙트 이벤트 수신: {}", message);
                ack.acknowledge();
                return;
            }

            String type = envelope.getEventType();
            if (!ALLOWED_SMARTCONTRACT_EVENTS.contains(type)) {
                log.info("무시된 Kafka 스마트컨트랙트 이벤트: eventType={}, eventId={}", type, envelope.getEventId());
                ack.acknowledge();
                return;
            }

            //스마트컨트랙트 payload → NotificationPayload 로 변환
            SmartContractPayload scPayload = envelope.getPayload();
            NotificationPayload notificationPayload = convertToNotificationPayload(type, scPayload);

            EventEnvelope<NotificationPayload> newEnvelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(envelope.getEventId())
                    .eventType(type)
                    .timestamp(envelope.getTimestamp())
                    .payload(notificationPayload)
                    .build();

            notificationService.handleNotificationEvent(newEnvelope);
            ack.acknowledge();
            log.info("Kafka 스마트컨트랙트 이벤트 처리 완료: eventId={}", envelope.getEventId());

        } catch (Exception e) {
            log.error("Kafka 스마트컨트랙트 이벤트 처리 실패: {}", message, e);
        }
    }

    private NotificationPayload convertToNotificationPayload(String type, SmartContractPayload scPayload) {
        return switch (type) {
            case "INVESTMENT.SUCCEEDED" -> NotificationPayload.builder()
                    .userSeq(null)
                    .notificationType(type)
                    .title("투자 성공")
                    .message("프로젝트 투자가 완료되었습니다.")
                    .build();

            case "INVESTMENT.FAILED" -> NotificationPayload.builder()
                    .userSeq(null)
                    .notificationType(type)
                    .title("투자 실패")
                    .message("프로젝트 투자를 실패했습니다.")
                    .build();

            case "TRADE.SUCCEEDED" -> NotificationPayload.builder()
                    .userSeq(null)
                    .notificationType(type)
                    .title("거래 성공")
                    .message("거래를 성공적으로 완료되었습니다.")
                    .build();

            case "TRADE.FAILED" -> NotificationPayload.builder()
                    .userSeq(null)
                    .notificationType(type)
                    .title("거래 실패")
                    .message("거래를 실패했습니다.")
                    .build();

            default -> NotificationPayload.builder()
                    .userSeq(null)
                    .notificationType(type)
                    .title("알 수 없는 이벤트")
                    .message("이벤트를 처리할 수 없습니다.")
                    .build();
        };
    }
}

package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
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
        handleEvent(message, ack, "알림", false);
    }

    @KafkaListener(topics = SMARTCONTRACT_TOPIC, groupId = "BackendNotification")
    public void smartContractConsume(String message, Acknowledgment ack) {
        handleEvent(message, ack, "스마트컨트랙트", true);
    }

    private void handleEvent(String message, Acknowledgment ack, String eventType, boolean isSmartContract) {
        try {
            // EventEnvelope<JsonNode>로 읽기
            EventEnvelope<JsonNode> envelope = objectMapper.readValue(
                    message,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, JsonNode.class)
            );

            if (envelope == null || envelope.getPayload() == null) {
                log.warn("잘못된 Kafka {} 이벤트 수신: {}", eventType, envelope);
                ack.acknowledge();
                return;
            }

            JsonNode payloadNode = envelope.getPayload();

            if (isSmartContract) {
                String type = envelope.getEventType(); // eventType 사용
                if (!ALLOWED_SMARTCONTRACT_EVENTS.contains(type)) {
                    log.info("무시된 Kafka {} 이벤트: eventType={}, eventId={}", eventType, type, envelope.getEventId());
                    ack.acknowledge();
                    return;
                }

                // 필요한 필드 추출 (존재하면)
                String projectId = payloadNode.has("projectId") ? payloadNode.get("projectId").asText() : null;
                Long investmentId = payloadNode.has("investmentId") ? payloadNode.get("investmentId").asLong() : null;
                Long tradeId = payloadNode.has("tradeId") ? payloadNode.get("tradeId").asLong() : null;
                String status = payloadNode.has("status") ? payloadNode.get("status").asText() : null;
                String reason = payloadNode.has("reason") ? payloadNode.get("reason").asText() : null;
                String errorType = payloadNode.has("errorType") ? payloadNode.get("errorType").asText() : null;
                String errorMessage = payloadNode.has("errorMessage") ? payloadNode.get("errorMessage").asText() : null;

                // 제목/메시지 설정
                String title = "";
                String messageText = "";
                switch (type) {
                    case "INVESTMENT.SUCCEEDED":
                        title = "투자 성공";
                        messageText = "프로젝트 투자가 완료되었습니다.";
                        break;
                    case "INVESTMENT.FAILED":
                        title = "투자 실패";
                        messageText = reason != null ? reason : "프로젝트 투자를 실패했습니다.";
                        break;
                    case "TRADE.SUCCEEDED":
                        title = "거래 성공";
                        messageText = "거래를 성공적으로 완료했습니다.";
                        break;
                    case "TRADE.FAILED":
                        title = "거래 실패";
                        messageText = errorMessage != null ? errorMessage : "거래를 실패했습니다.";
                        break;
                }

                // NotificationPayload 생성
                NotificationPayload notificationPayload = NotificationPayload.builder()
                        .userSeq(null) // 필요 시 추출
                        .notificationType(type)
                        .title(title)
                        .message(messageText)
                        .build();

                EventEnvelope<NotificationPayload> newEnvelope = EventEnvelope.<NotificationPayload>builder()
                        .eventId(envelope.getEventId())
                        .timestamp(envelope.getTimestamp())
                        .payload(notificationPayload)
                        .build();

                notificationService.handleNotificationEvent(newEnvelope);
                ack.acknowledge();
                log.info("Kafka {} 이벤트 처리 완료: eventId={}", eventType, envelope.getEventId());
                return;
            }

            // 일반 알림 처리
            NotificationPayload payload = objectMapper.treeToValue(payloadNode, NotificationPayload.class);
            EventEnvelope<NotificationPayload> newEnvelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(envelope.getEventId())
                    .timestamp(envelope.getTimestamp())
                    .payload(payload)
                    .build();

            notificationService.handleNotificationEvent(newEnvelope);
            ack.acknowledge();
            log.info("Kafka {} 이벤트 처리 완료: eventId={}", eventType, envelope.getEventId());

        } catch (Exception e) {
            log.error("Kafka {} 이벤트 처리 실패: {}", eventType, message, e);
        }
    }
}
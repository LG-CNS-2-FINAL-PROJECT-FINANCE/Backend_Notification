package com.ddiring.Backend_Notification.kafka;

import com.ddiring.Backend_Notification.service.NotificationService;
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

    private void handleEvent(String message, Acknowledgment ack, String eventType, boolean filterEventType) {
        try {
            EventEnvelope<NotificationPayload> envelope = objectMapper
                    .readValue(message, objectMapper.getTypeFactory()
                            .constructParametricType(EventEnvelope.class, NotificationPayload.class));

            if (envelope == null || envelope.getPayload() == null) {
                log.warn("잘못된 Kafka {} 이벤트 수신: {}", eventType, envelope);
                ack.acknowledge();
                return;
            }

            NotificationPayload payload = envelope.getPayload();

            //스마트컨트랙트 이벤트 필터링 + 맞춤 payload 생성
            if (filterEventType) {
                String type = payload.getNotificationType();

                if (!ALLOWED_SMARTCONTRACT_EVENTS.contains(type)) {
                    log.info("무시된 Kafka {} 이벤트: eventType={}, eventId={}", eventType, type, envelope.getEventId());
                    ack.acknowledge();
                    return;
                }

                //이벤트 타입별 title/message 재설정
                switch (type) {
                    case "INVESTMENT.SUCCEEDED":
                        payload = payload.withTitleAndMessage(
                                "투자 성공",
                                "프로젝트 투자가 완료되었습니다."
                        );
                        break;
                    case "INVESTMENT.FAILED":
                        payload = payload.withTitleAndMessage(
                                "투자 실패",
                                "프로젝트 투자를 실패했습니다."
                        );
                        break;
                    case "TRADE.SUCCEEDED":
                        payload = payload.withTitleAndMessage(
                                "거래 성공",
                                "거래를 성공적으로 완료되었습니다."
                        );
                        break;
                    case "TRADE.FAILED":
                        payload = payload.withTitleAndMessage(
                                "거래 실패",
                                "거래를 실패했습니다."
                        );
                        break;
                }

                //새로운 envelope 생성
                envelope = EventEnvelope.<NotificationPayload>builder()
                        .eventId(envelope.getEventId())
                        .timestamp(envelope.getTimestamp())
                        .payload(payload)
                        .build();
            }

            notificationService.handleNotificationEvent(envelope);
            ack.acknowledge();
            log.info("Kafka {} 이벤트 처리 완료: eventId={}", eventType, envelope.getEventId());

        } catch (Exception e) {
            log.error("Kafka {} 이벤트 처리 실패: {}", eventType, message, e);
        }
    }
}
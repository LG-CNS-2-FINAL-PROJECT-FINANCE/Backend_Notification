package com.ddiring.Backend_Notification.service;

import com.ddiring.Backend_Notification.Entity.Notification;
import com.ddiring.Backend_Notification.Entity.UserNotification;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.dto.response.NotificationResponse;
import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.enums.NotificationStatus;
import com.ddiring.Backend_Notification.enums.NotificationType;
import com.ddiring.Backend_Notification.kafka.EventEnvelope;
import com.ddiring.Backend_Notification.kafka.NotificationPayload;
import com.ddiring.Backend_Notification.repository.NotificationRepository;
import com.ddiring.Backend_Notification.repository.UserNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String DLQ_TOPIC = "notification-dlq";

    //userSeq별 SSE Emitter 저장
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    //단일 사용자 SSE 연결
    public SseEmitter connectForUser(String userSeq) {
        return connectForUsers(List.of(userSeq));
    }

    public SseEmitter connectForUsers(List<String> userSeqList) {
        log.info("🔌 [SSE 연결 시도] 대상 userSeqList={}", userSeqList);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        for (String userSeq : userSeqList) {
            emitters.computeIfAbsent(userSeq, k -> new CopyOnWriteArrayList<>()).add(emitter);
            log.info("✅ emitter 등록 완료 userSeq={}", userSeq);
        }

        // 연결 종료 / 타임아웃 이벤트
        emitter.onCompletion(() -> removeEmitters(userSeqList, emitter));
        emitter.onTimeout(() -> removeEmitters(userSeqList, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            log.info("📡 [SSE 연결 성공 이벤트 전송] userSeqList={}", userSeqList);
        } catch (Exception e) {
            log.error("❌ [SSE 초기 연결 실패] userSeqList={}, error={}", userSeqList, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        // Heartbeat 전송 (30초마다)
        Timer heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (Exception e) {
                    emitter.complete();
                    heartbeatTimer.cancel();
                }
            }
        }, 0, 30_000);

        return emitter;
    }

    private void removeEmitters(List<String> userSeqList, SseEmitter emitter) {
        for (String seq : userSeqList) {
            emitters.getOrDefault(seq, List.of()).remove(emitter);
        }
    }

    //알림 전송(SSE) userSeq에 연결이 없으면 DLQ로 이동
    public void sendNotification(List<String> userSeqList, NotificationPayload payload) {
        for (String userSeq : userSeqList) {
            List<SseEmitter> userEmitters = emitters.get(userSeq);
            if (userEmitters == null || userEmitters.isEmpty()) {
                sendToDLQ(payload);
                continue;
            }

            for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
                try {
                    // payload를 EventEnvelope 형태로 감싸서 전송
                    EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                            .eventId(UUID.randomUUID().toString())
                            .timestamp(Instant.now())
                            .payload(payload)
                            .build();

                    String json = objectMapper.writeValueAsString(envelope);
                    emitter.send(SseEmitter.event().name("Notification").data(json));
                } catch (Exception e) {
                    userEmitters.remove(emitter);
                    emitter.completeWithError(e);
                    sendToDLQ(payload);
                }
            }
        }
    }

    //SSE 전송 실패 시 DLQ
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

    /**
     * Kafka Consumer 또는 서비스에서 호출
     * - Notification DB 저장
     * - UserNotification DB 저장
     * - SSE 전송
     * - 실패 시 DLQ 이동
     */
    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        NotificationPayload payload = envelope.getPayload();
        LocalDateTime now = LocalDateTime.now();

        //Notification 저장
        Notification notification = Notification.builder()
                .eventId(envelope.getEventId())
                .notificationType(NotificationType.valueOf(payload.getNotificationType()))
                .title(payload.getTitle())
                .message(payload.getMessage())
                .createdId("system").createdAt(now)
                .updatedId("system").updatedAt(now)
                .build();
        notificationRepository.save(notification);

        //UserNotification 저장
        List<String> userSeqList = payload.getUserSeq();
        if (userSeqList == null || userSeqList.isEmpty()) return;

        for (String userSeq : userSeqList) {
            UserNotification un = UserNotification.builder()
                    .notification(notification)
                    .userSeq(userSeq)
                    .notificationStatus(NotificationStatus.UNREAD)
                    .sentAt(now)
                    .createdId("system").createdAt(now)
                    .updatedId("system").updatedAt(now)
                    .build();
            userNotificationRepository.save(un);
        }

        //SSE 전송
        sendNotification(userSeqList, payload);
    }

    //사용자 알림 조회
    public List<UserNotificationResponse> getUserNotifications(String userSeq) {
        return userNotificationRepository.findAllWithNotificationByUserSeq(userSeq).stream()
                .map(n -> UserNotificationResponse.builder()
                        .userNotificationSeq(n.getUserNotificationSeq())
                        .userSeq(n.getUserSeq())
                        .sentAt(n.getSentAt())
                        .notificationStatus(n.getNotificationStatus())
                        .readAt(n.getReadAt())
                        .notification(NotificationResponse.builder()
                                .notificationSeq(n.getNotification().getNotificationSeq())
                                .notificationType(n.getNotification().getNotificationType())
                                .title(n.getNotification().getTitle())
                                .message(n.getNotification().getMessage())
                                .createdAt(n.getNotification().getCreatedAt())
                                .build())
                        .build())
                .toList();
    }

    //알림 읽음 처리
    public void markAsRead(String userSeq, MarkAsReadRequest request) {
        List<UserNotification> list =
                userNotificationRepository.findAllByUserSeqAndIds(userSeq, request.getUserNotificationSeqs());

        LocalDateTime now = LocalDateTime.now();
        list.forEach(n -> {
            n.setNotificationStatus(NotificationStatus.READ);
            n.setReadAt(now);
        });
        userNotificationRepository.saveAll(list);
    }
}
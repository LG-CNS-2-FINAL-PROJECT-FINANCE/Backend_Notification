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

    // userSeq별 SSE Emitter 저장
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 단일 사용자 SSE 연결
    public SseEmitter connectForUser(String userSeq) {
        return connectForUsers(List.of(userSeq));
    }

    // 다중 사용자 SSE 연결
    public SseEmitter connectForUsers(List<String> userSeqList) {
        log.info("🔌 [SSE 연결 시도] 대상 userSeqList={}", userSeqList);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        for (String userSeq : userSeqList) {
            emitters.computeIfAbsent(userSeq, k -> new CopyOnWriteArrayList<>()).add(emitter);
            log.info("✅ [emitter 등록] userSeq={}, 현재 등록된 emitter 수={}", userSeq, emitters.get(userSeq).size());
        }

        // 연결 종료 / 타임아웃 이벤트
        emitter.onCompletion(() -> {
            log.info("🗑 [SSE 연결 종료 이벤트] userSeqList={}", userSeqList);
            removeEmitters(userSeqList, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("⏱ [SSE 연결 타임아웃] userSeqList={}", userSeqList);
            removeEmitters(userSeqList, emitter);
        });

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            log.info("📡 [SSE 연결 성공 이벤트 전송] userSeqList={}", userSeqList);
        } catch (Exception e) {
            log.error("❌ [SSE 초기 연결 실패] userSeqList={}, error={}", userSeqList, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        // Heartbeat 전송 (15초마다)
        Timer heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    log.debug("💓 [heartbeat 전송] userSeqList={}, emitter={}", userSeqList, emitter);
                } catch (Exception e) {
                    log.error("❌ [heartbeat 전송 실패] userSeqList={}, error={}", userSeqList, e.getMessage());
                    emitter.complete();
                    heartbeatTimer.cancel();
                }
            }
        }, 0, 15_000);

        return emitter;
    }

    private void removeEmitters(List<String> userSeqList, SseEmitter emitter) {
        for (String seq : userSeqList) {
            List<SseEmitter> list = emitters.getOrDefault(seq, List.of());
            list.remove(emitter);
            log.info("🗑 [emitter 제거] userSeq={}, 남은 emitter 수={}", seq, list.size());
        }
    }

    // 알림 전송(SSE) userSeq에 연결이 없으면 DLQ로 이동
    public void sendNotification(List<String> userSeqList, NotificationPayload payload) {
        log.info("✉️ [알림 전송 시작] userSeqList={}, payload={}", userSeqList, payload);

        for (String userSeq : userSeqList) {
            List<SseEmitter> userEmitters = emitters.get(userSeq);
            log.info("🔎 [emitters 조회] userSeq={}, emitter 수={}", userSeq, userEmitters != null ? userEmitters.size() : 0);

            if (userEmitters == null || userEmitters.isEmpty()) {
                log.warn("⚠️ [SSE 미연결 사용자] userSeq={} → DLQ 이동", userSeq);
                sendToDLQ(payload);
                continue;
            }

            for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
                try {
                    EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                            .eventId(UUID.randomUUID().toString())
                            .timestamp(Instant.now())
                            .payload(payload)
                            .build();

                    String json = objectMapper.writeValueAsString(envelope);
                    emitter.send(SseEmitter.event().name("Notification").data(json));
                    log.info("📤 [SSE 전송 완료] userSeq={}, eventId={}, emitter={}, payload={}", userSeq, envelope.getEventId(), emitter, payload);
                } catch (Exception e) {
                    log.error("❌ [SSE 전송 실패] userSeq={}, error={}", userSeq, e.getMessage(), e);
                    userEmitters.remove(emitter);
                    emitter.completeWithError(e);
                    sendToDLQ(payload);
                }
            }
        }
    }

    // SSE 전송 실패 시 DLQ
    private void sendToDLQ(NotificationPayload payload) {
        try {
            EventEnvelope<NotificationPayload> envelope = EventEnvelope.<NotificationPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .payload(payload)
                    .build();
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(DLQ_TOPIC, json);
            log.warn("📦 [DLQ 전송 완료] payload={}", payload);
        } catch (Exception e) {
            log.error("❌ [DLQ 전송 실패] payload={}, error={}", payload, e.getMessage(), e);
        }
    }

    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        NotificationPayload payload = envelope.getPayload();
        LocalDateTime now = LocalDateTime.now();

        log.info("🎯 [Kafka 이벤트 수신] eventId={}, payload={}", envelope.getEventId(), payload);

        // Notification 저장
        Notification notification = Notification.builder()
                .eventId(envelope.getEventId())
                .notificationType(NotificationType.valueOf(payload.getNotificationType()))
                .title(payload.getTitle())
                .message(payload.getMessage())
                .createdId("system").createdAt(now)
                .updatedId("system").updatedAt(now)
                .build();
        notificationRepository.save(notification);
        log.info("💾 [Notification 저장 완료] eventId={}", envelope.getEventId());

        // UserNotification 저장
        List<String> userSeqList = payload.getUserSeq();
        if (userSeqList == null || userSeqList.isEmpty()) {
            log.warn("⚠️ [UserNotification 대상 없음] eventId={}", envelope.getEventId());
            return;
        }

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
            log.info("💾 [UserNotification 저장 완료] userSeq={}, eventId={}", userSeq, envelope.getEventId());
        }

        // SSE 전송
        sendNotification(userSeqList, payload);
    }

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

    public void markAsRead(String userSeq, MarkAsReadRequest request) {
        List<UserNotification> list =
                userNotificationRepository.findAllByUserSeqAndIds(userSeq, request.getUserNotificationSeqs());

        LocalDateTime now = LocalDateTime.now();
        list.forEach(n -> {
            n.setNotificationStatus(NotificationStatus.READ);
            n.setReadAt(now);
        });
        userNotificationRepository.saveAll(list);
        log.info("✅ [알림 읽음 처리 완료] userSeq={}, count={}", userSeq, list.size());
    }
}

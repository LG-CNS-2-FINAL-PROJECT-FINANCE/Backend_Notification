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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    // userSeq별 emitter (1명당 1개만 유지)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결
    public SseEmitter connect(String userSeq) {
        log.info("🔌 [SSE 연결 시도] userSeq={}", userSeq);

        // 기존 emitter 있으면 제거
        if (emitters.containsKey(userSeq)) {
            log.info("♻️ [기존 emitter 제거 후 재등록] userSeq={}", userSeq);
            emitters.get(userSeq).complete();
            emitters.remove(userSeq);
        }

        // 새로운 emitter 등록
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userSeq, emitter);

        emitter.onCompletion(() -> removeEmitter(userSeq));
        emitter.onTimeout(() -> removeEmitter(userSeq));
        emitter.onError((e) -> removeEmitter(userSeq));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            log.error("❌ [SSE 초기 연결 실패] userSeq={}, error={}", userSeq, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void removeEmitter(String userSeq) {
        emitters.remove(userSeq);
        log.info("🗑 [emitter 제거] userSeq={}", userSeq);
    }

    // Kafka Event 처리 (DB 저장 + SSE 전송)
    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        NotificationPayload payload = envelope.getPayload();
        List<String> userSeqList = payload.getUserSeq();
        LocalDateTime now = LocalDateTime.now();

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

        // UserNotification 저장
        if (userSeqList != null && !userSeqList.isEmpty()) {
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
        }

        // SSE 전송
        sendNotification(userSeqList, envelope);
    }

    // SSE 전송
    public void sendNotification(List<String> userSeqList, EventEnvelope<NotificationPayload> envelope) {
        for (String userSeq : userSeqList) {
            SseEmitter emitter = emitters.get(userSeq);

            if (emitter == null) {
                log.warn("⚠️ [SSE 미연결 사용자] userSeq={} → 알림 미전송", userSeq);
                continue;
            }

            try {
                emitter.send(SseEmitter.event().name("notification").data(envelope));
                log.info("📤 [SSE 전송 완료] userSeq={}, title={}", userSeq, envelope.getPayload().getTitle());
            } catch (Exception e) {
                log.error("❌ [SSE 전송 실패] userSeq={}, error={}", userSeq, e.getMessage(), e);
                removeEmitter(userSeq);
                emitter.completeWithError(e);
            }
        }
    }

    // 유저 알림 조회
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

    // 읽음 처리
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
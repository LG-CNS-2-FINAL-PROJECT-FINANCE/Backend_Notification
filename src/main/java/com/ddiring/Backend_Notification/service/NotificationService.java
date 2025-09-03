package com.ddiring.Backend_Notification.service;

import com.ddiring.Backend_Notification.Entity.Notification;
import com.ddiring.Backend_Notification.Entity.UserNotification;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.dto.response.NotificationResponse;
import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.enums.NotificationStatus;
import com.ddiring.Backend_Notification.enums.NotificationType;
import com.ddiring.Backend_Notification.kafka.NotificationEvent;
import com.ddiring.Backend_Notification.repository.NotificationRepository;
import com.ddiring.Backend_Notification.repository.UserNotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;  // Object 타입 사용

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

        emitter.onCompletion(() -> removeEmitters(userSeqList, emitter));
        emitter.onTimeout(() -> removeEmitters(userSeqList, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            log.info("📡 [SSE 연결 성공 이벤트 전송] userSeqList={}", userSeqList);
        } catch (Exception e) {
            log.error("❌ [SSE 초기 연결 실패] userSeqList={}, error={}", userSeqList, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void removeEmitters(List<String> userSeqList, SseEmitter emitter) {
        for (String seq : userSeqList) {
            List<SseEmitter> list = emitters.getOrDefault(seq, List.of());
            list.remove(emitter);
            log.info("🗑 [emitter 제거] userSeq={}, 남은 emitter 수={}", seq, list.size());
        }
    }

    // 알림 전송(SSE)
    public void sendNotification(List<String> userSeqList, NotificationEvent event) {
        log.info("✉️ [알림 전송 시작] userSeqList={}, event={}", userSeqList, event);

        for (String userSeq : userSeqList) {
            List<SseEmitter> userEmitters = emitters.get(userSeq);
            if (userEmitters == null || userEmitters.isEmpty()) {
                log.warn("⚠️ [SSE 미연결 사용자] userSeq={} → 알림 미전송", userSeq);
                continue;
            }

            for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
                try {
                    emitter.send(SseEmitter.event().name("Notification").data(event));
                    log.info("📤 [SSE 전송 완료] userSeq={}, eventId={}", userSeq, event.getEventId());
                } catch (Exception e) {
                    log.error("❌ [SSE 전송 실패] userSeq={}, error={}", userSeq, e.getMessage(), e);
                    userEmitters.remove(emitter);
                    emitter.completeWithError(e);
                }
            }
        }
    }

    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        LocalDateTime now = LocalDateTime.now();
        log.info("🎯 [Kafka 이벤트 수신] eventId={}, event={}", event.getEventId(), event);

        // Notification 저장
        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .notificationType(NotificationType.valueOf(event.getNotificationType()))
                .title(event.getTitle())
                .message(event.getMessage())
                .createdId("system").createdAt(now)
                .updatedId("system").updatedAt(now)
                .build();
        notificationRepository.save(notification);

        // UserNotification 저장
        List<String> userSeqList = event.getUserSeq();
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
        sendNotification(userSeqList, event);
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
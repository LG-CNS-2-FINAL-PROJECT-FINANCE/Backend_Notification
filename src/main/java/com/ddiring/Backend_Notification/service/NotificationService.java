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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final int MAX_EMITTER_PER_USER = 3; // 다중 탭 허용 시 최대 emitter 수 제한

    // SSE 연결
    public SseEmitter connectForUsers(List<String> userSeqList) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        for (String userSeq : userSeqList) {
            emitters.computeIfAbsent(userSeq, k -> Collections.synchronizedSet(new HashSet<>()));
            Set<SseEmitter> userEmitters = emitters.get(userSeq);

            // 1) 기존 emitter 모두 종료하고 제거 (중복 방지)
            synchronized (userEmitters) {
                Iterator<SseEmitter> it = userEmitters.iterator();
                while (it.hasNext()) {
                    SseEmitter e = it.next();
                    try { e.complete(); } catch (Exception ignored) {}
                    it.remove();
                }

                // 2) 새 emitter 등록
                userEmitters.add(emitter);

                // 3) 다중 탭 허용 시 max emitter 수 제한
                if (userEmitters.size() > MAX_EMITTER_PER_USER) {
                    Iterator<SseEmitter> overflowIt = userEmitters.iterator();
                    while (userEmitters.size() > MAX_EMITTER_PER_USER && overflowIt.hasNext()) {
                        SseEmitter old = overflowIt.next();
                        try { old.complete(); } catch (Exception ignored) {}
                        overflowIt.remove();
                    }
                }
            }

            log.info("✅ [emitter 등록] userSeq={}, 현재 등록된 emitter 수={}", userSeq, userEmitters.size());
        }

        emitter.onCompletion(() -> removeEmitters(userSeqList, emitter));
        emitter.onTimeout(() -> removeEmitters(userSeqList, emitter));
        emitter.onError((e) -> removeEmitters(userSeqList, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            log.error("❌ [SSE 초기 연결 실패] userSeqList={}, error={}", userSeqList, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void removeEmitters(List<String> userSeqList, SseEmitter emitter) {
        for (String seq : userSeqList) {
            Set<SseEmitter> userEmitters = emitters.get(seq);
            if (userEmitters != null) {
                synchronized (userEmitters) {
                    userEmitters.remove(emitter);
                    log.info("🗑 [emitter 제거] userSeq={}, 남은 emitter 수={}", seq, userEmitters.size());
                    if (userEmitters.isEmpty()) emitters.remove(seq);
                }
            }
        }
    }

    // Kafka Event 처리 + SSE 전송
    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        NotificationPayload payload = envelope.getPayload();
        List<String> userSeqList = payload.getUserSeq();
        LocalDateTime now = LocalDateTime.now();

        Notification notification = Notification.builder()
                .eventId(envelope.getEventId())
                .notificationType(NotificationType.valueOf(payload.getNotificationType()))
                .title(payload.getTitle())
                .message(payload.getMessage())
                .createdId("system").createdAt(now)
                .updatedId("system").updatedAt(now)
                .build();
        notificationRepository.save(notification);

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

        sendNotification(userSeqList, envelope);
    }

    public void sendNotification(List<String> userSeqList, EventEnvelope<NotificationPayload> envelope) {
        for (String userSeq : userSeqList) {
            Set<SseEmitter> userEmitters = emitters.get(userSeq);
            if (userEmitters == null || userEmitters.isEmpty()) continue;

            List<SseEmitter> copyEmitters;
            synchronized (userEmitters) {
                copyEmitters = new ArrayList<>(userEmitters);
            }

            for (SseEmitter emitter : copyEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(envelope));
                } catch (Exception e) {
                    log.warn("❌ [SSE 전송 실패] userSeq={}, error={}", userSeq, e.getMessage());
                    removeEmitters(Collections.singletonList(userSeq), emitter);
                    emitter.completeWithError(e);
                }
            }
        }
    }

    @PostConstruct
    public void initHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Set<SseEmitter>> entry : emitters.entrySet()) {
                String userSeq = entry.getKey();
                Set<SseEmitter> userEmitters = entry.getValue();
                List<SseEmitter> copyEmitters;
                synchronized (userEmitters) { copyEmitters = new ArrayList<>(userEmitters); }

                for (SseEmitter emitter : copyEmitters) {
                    try {
                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                                .name("heartbeat")
                                .data("ping")
                                .id(String.valueOf(System.currentTimeMillis()))
                                .reconnectTime(15000);
                        emitter.send(event);
                        log.info("💓 [SSE heartbeat 전송] userSeq={}", userSeq);
                    } catch (Exception e) {
                        log.warn("❌ [SSE heartbeat 전송 실패] userSeq={}, error={}", userSeq, e.getMessage());
                        removeEmitters(Collections.singletonList(userSeq), emitter);
                        emitter.completeWithError(e);
                    }
                }
            }
        }, 0, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdownScheduler() { scheduler.shutdown(); }

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
    }
}
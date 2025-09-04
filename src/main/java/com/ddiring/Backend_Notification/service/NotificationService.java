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

    // userSeq별 SSE Emitter 저장
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // SSE 연결
    public SseEmitter connectForUsers(List<String> userSeqList) {
        log.info("🔌 [SSE 연결 시도] 대상 userSeqList={}", userSeqList);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        for (String userSeq : userSeqList) {
            emitters.computeIfAbsent(userSeq, k -> Collections.synchronizedSet(new HashSet<>()));

            Set<SseEmitter> userEmitters = emitters.get(userSeq);
            if (!userEmitters.contains(emitter)) { // 중복 등록 방지
                userEmitters.add(emitter);
                log.info("✅ [emitter 등록] userSeq={}, 현재 등록된 emitter 수={}", userSeq, userEmitters.size());
            }
        }

        // 완료/타임아웃 시 emitter 제거
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
                userEmitters.remove(emitter);
                log.info("🗑 [emitter 제거] userSeq={}, 남은 emitter 수={}", seq, userEmitters.size());
                if (userEmitters.isEmpty()) {
                    emitters.remove(seq);
                }
            }
        }
    }

    // Kafka Event 처리 DB저장 + SSE 전송
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
            Set<SseEmitter> userEmitters = emitters.get(userSeq);
            if (userEmitters == null || userEmitters.isEmpty()) continue;

            // 동기화된 Set 복사
            List<SseEmitter> emittersToSend = new ArrayList<>(userEmitters);
            for (SseEmitter emitter : emittersToSend) {
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

    // 생성자나 init 메서드에서 스케줄러 시작
    @PostConstruct
    public void initHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Set<SseEmitter>> entry : emitters.entrySet()) {
                String userSeq = entry.getKey();
                Set<SseEmitter> userEmitters = entry.getValue();

                List<SseEmitter> emittersToSend = new ArrayList<>(userEmitters);
                for (SseEmitter emitter : emittersToSend) {
                    try {
                        emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    } catch (Exception e) {
                        log.warn("❌ [SSE heartbeat 전송 실패] userSeq={}, error={}", userSeq, e.getMessage());
                        removeEmitters(Collections.singletonList(userSeq), emitter);
                        emitter.completeWithError(e);
                    }
                }
            }
        }, 0, 15, TimeUnit.SECONDS); // 15초마다 heartbeat 전송
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduler.shutdown();
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
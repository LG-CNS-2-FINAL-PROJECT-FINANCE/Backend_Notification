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
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    //SSE 연결
    public SseEmitter connectForUsers(List<String> userSeqs) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        for (String userSeq : userSeqs) {
            emitters.computeIfAbsent(userSeq, k -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        emitter.onCompletion(() -> userSeqs.forEach(seq -> emitters.getOrDefault(seq, List.of()).remove(emitter)));
        emitter.onTimeout(() -> userSeqs.forEach(seq -> emitters.getOrDefault(seq, List.of()).remove(emitter)));

        return emitter;
    }

    //SSE 전송
    public void sendNotification(String userSeq, NotificationPayload payload) {
        List<SseEmitter> userEmitters = emitters.get(userSeq);
        if (userEmitters == null) return;

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("Notification")
                        .data(payload));
            } catch (Exception e) {
                //실패하면 emitter 제거
                userEmitters.remove(emitter);
            }
        }
    }

    //알림 저장 + 전송
    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        NotificationPayload payload = envelope.getPayload();
        LocalDateTime now = LocalDateTime.now();

        //Notification 저장
        Notification notification = Notification.builder()
                .eventId(envelope.getEventId())
                .notificationType(NotificationType.valueOf(payload.getNotificationType()))
                .message(payload.getMessage())
                .createdId("system")
                .createdAt(now)
                .updatedId("system")
                .updatedAt(now)
                .build();
        notificationRepository.save(notification);

        //UserNotification 저장 + SSE 전송
        for (String userSeq : payload.getUserSeqs()) {
            UserNotification userNotification = UserNotification.builder()
                    .notification(notification)
                    .userSeq(userSeq)
                    .notificationStatus(NotificationStatus.UNREAD)
                    .sentAt(now)
                    .createdId("system")
                    .createdAt(now)
                    .updatedId("system")
                    .updatedAt(now)
                    .build();
            userNotificationRepository.save(userNotification);

            //사용자별 SSE 전송
            sendNotification(userSeq, payload);
        }
    }

    //사용자 알림 리스트 조회
    public List<UserNotificationResponse> getUserNotifications(String userSeq) {
        List<UserNotification> notifications = userNotificationRepository.findAllByUserSeq(userSeq);

        return notifications.stream()
                .map(n -> UserNotificationResponse.builder()
                        .userNotificationSeq(n.getUserNotificationSeq())
                        .userSeq(n.getUserSeq())
                        .sentAt(n.getSentAt())
                        .notificationStatus(n.getNotificationStatus())
                        .readAt(n.getReadAt())
                        .notification(NotificationResponse.builder()
                                .notificationSeq(n.getNotification().getNotificationSeq())
                                .notificationType(n.getNotification().getNotificationType())
                                .message(n.getNotification().getMessage())
                                .createdAt(n.getNotification().getCreatedAt())
                                .build())
                        .build())
                .toList();
    }

    //알림 읽음 처리
    public void markAsRead(String userSeq, MarkAsReadRequest request) {
        List<UserNotification> notifications =
                userNotificationRepository.findAllByUserSeqAndIds(userSeq, request.getUserNotificationSeqs());

        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(n -> {
            n.setNotificationStatus(NotificationStatus.READ);
            n.setReadAt(now);
        });
    }

}
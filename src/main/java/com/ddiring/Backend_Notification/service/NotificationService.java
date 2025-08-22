package com.ddiring.Backend_Notification.service;

import com.ddiring.Backend_Notification.Entity.Notification;
import com.ddiring.Backend_Notification.Entity.UserNotification;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.dto.response.NotificationResponse;
import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.enums.NotificationStatus;
import com.ddiring.Backend_Notification.enums.NotificationType;
import com.ddiring.Backend_Notification.kafka.NotificationPayload;
import com.ddiring.Backend_Notification.repository.NotificationRepository;
import com.ddiring.Backend_Notification.repository.UserNotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    //SSE 연결
    public SseEmitter connect(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        return emitter;
    }

    //SSE 전송
    public void sendNotification(Integer userSeq, NotificationPayload payload) {
        SseEmitter emitter = emitters.get(String.valueOf(userSeq));
        System.out.println("SSE 전송 시도: userSeq=" + userSeq + ", emitter=" + emitter);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("Notification")
                        .data(payload));
                System.out.println("📤 SSE 전송 완료: " + payload);
            } catch (Exception e) {
                emitters.remove(String.valueOf(userSeq));
            }
        }
    }

    //알림 저장 + 전송
    @Transactional
    public void handleNotificationEvent(NotificationPayload payload) {
        LocalDateTime now = LocalDateTime.now();

        //Notification 저장
        Notification notification = Notification.builder()
                .notificationType(NotificationType.valueOf(payload.getNotificationType()))
                .message(payload.getMessage())
                .createdId("system")
                .createdAt(now)
                .updatedId("system")
                .updatedAt(now)
                .build();
        notificationRepository.save(notification);

        //UserNotification 저장 + SSE 전송
        for (Integer userSeq : payload.getUserSeqs()) {
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
    public List<UserNotificationResponse> getUserNotifications(Integer userSeq) {
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
                .collect(Collectors.toList());
    }

//    //알림 읽음 처리
//    public void markAsRead(MarkAsReadRequest request, Integer userSeq) {
//        List<UserNotification> notifications =
//                userNotificationRepository.findAllByUserSeqAndIds(userSeq, request.getUserNotificationSeqs());
//
//        LocalDateTime now = LocalDateTime.now();
//
//        notifications.forEach(n -> {
//            n.setNotificationStatus(NotificationStatus.READ);
//            n.setReadAt(now);
//        });
//    }

}
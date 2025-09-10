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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserDeviceTokenService userDeviceTokenService;
    private final FcmService fcmService;

    @Transactional
    public void handleNotificationEvent(EventEnvelope<NotificationPayload> envelope) {
        if (notificationRepository.existsByEventId(envelope.getEventId())) {
            log.warn("중복 이벤트 수신: eventId={}", envelope.getEventId());
            return;
        }

        NotificationPayload payload = envelope.getPayload();
        List<String> userSeqList = payload.getUserSeq();
        LocalDateTime now = LocalDateTime.now();

        // 이벤트 타입 문자열 → DB 저장용 enum 변환
        NotificationType notificationType;
        String eventType = payload.getNotificationType();

        switch (eventType) {
            case "INVESTMENT.SUCCEEDED":
            case "TRADE.SUCCEEDED":
                notificationType = NotificationType.INFORMATION;
                break;
            case "INVESTMENT.FAILED":
            case "TRADE.FAILED":
                notificationType = NotificationType.ERROR;
                break;
            default:
                notificationType = NotificationType.WARNING;
                break;
        }

        //Notification 저장
        Notification notification = Notification.builder()
                .eventId(envelope.getEventId())
                .notificationType(notificationType) // enum으로 저장
                .title(payload.getTitle())
                .message(payload.getMessage())
                .createdId("system").createdAt(now)
                .updatedId("system").updatedAt(now)
                .build();
        notificationRepository.save(notification);

        //UserNotification 저장 + FCM 전송
        if (userSeqList != null && !userSeqList.isEmpty()) {
            List<UserNotification> userNotifications = new ArrayList<>();

            for (String userSeq : userSeqList) {
                UserNotification userNotification = UserNotification.builder()
                        .notification(notification)
                        .userSeq(userSeq)
                        .notificationStatus(NotificationStatus.UNREAD)
                        .sentAt(now)
                        .createdId("system").createdAt(now)
                        .updatedId("system").updatedAt(now)
                        .build();
                userNotifications.add(userNotification);

                List<String> deviceTokens = userDeviceTokenService.getDeviceTokens(userSeq);
                for (String token : deviceTokens) {
                    try {
                        fcmService.send(token, payload.getTitle(), payload.getMessage());
                    } catch (Exception e) {
                        log.warn("FCM 전송 실패: userSeq={}, token={}, error={}",
                                userSeq, token, e.getMessage());
                    }
                }
            }

            userNotificationRepository.saveAll(userNotifications);
        }

        log.info("Notification 처리 완료: eventId={}, users={}", envelope.getEventId(), userSeqList);
    }

    //알림 리스트 조회
    public List<UserNotificationResponse> getUserNotifications(String userSeq) {
        return userNotificationRepository.findAllWithNotificationByUserSeqAndStatus(userSeq, NotificationStatus.UNREAD).stream()
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

    @Transactional
    public void markAsRead(String userSeq, MarkAsReadRequest request) {
        List<UserNotification> list = userNotificationRepository.findAllByUserSeqAndIds(userSeq, request.getUserNotificationSeqs());

        if (list.isEmpty()) {
            log.warn("읽음 처리할 알림 없음: userSeq={}, ids={}", userSeq, request.getUserNotificationSeqs());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        list.forEach(userNotification -> userNotification.markAsRead(userSeq, now));

        userNotificationRepository.saveAll(list);

        log.info("[알림 읽음 처리 완료] userSeq={}, count={}", userSeq, list.size());
    }

}
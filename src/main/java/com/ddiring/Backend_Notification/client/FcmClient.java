package com.ddiring.Backend_Notification.client;

import com.ddiring.Backend_Notification.dto.request.FcmRequest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmClient {

    private final FirebaseMessaging firebaseMessaging;

    public void send(FcmRequest request) {
        if (request.getDeviceToken() == null || request.getDeviceToken().isEmpty()) {
            log.warn("Device token 없음, FCM 발송 건너뜀");
            return;
        }

        Message fcmMessage = Message.builder()
                .setToken(request.getDeviceToken())
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .build();

        try {
            String response = firebaseMessaging.send(fcmMessage);
            log.info("FCM 전송 성공: token={}, response={}", request.getDeviceToken(), response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패: token={}, error={}", request.getDeviceToken(), e.getMessage(), e);
        }
    }
}
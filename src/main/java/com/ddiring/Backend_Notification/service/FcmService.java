package com.ddiring.Backend_Notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void send(String token, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("customData", "example")
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 전송 성공: token={}, response={}", token, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패: token={}, error={}", token, e.getMessage());
        }
    }
}
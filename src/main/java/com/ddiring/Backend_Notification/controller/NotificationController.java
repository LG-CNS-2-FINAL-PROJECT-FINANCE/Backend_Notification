package com.ddiring.Backend_Notification.controller;

import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.service.NotificationService;
import com.ddiring.Backend_Notification.common.util.GatewayRequestHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        log.info("🚀 [Controller] /api/notification/stream 호출됨");

        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        if (userSeq == null) {
            log.warn("⚠️ GatewayRequestHeaderUtils.getUserSeq() 값이 null임. 기본값 사용");
            userSeq = "anonymous";
        }
        log.info("🔥 [SSE 요청 수신] userSeq={}", userSeq);

        return notificationService.connectForUsers(Collections.singletonList(userSeq));
    }

    @GetMapping("/list")
    public ResponseEntity<List<UserNotificationResponse>> getUserNotifications() {
        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        if (userSeq == null) {
            log.warn("⚠️ userSeq null. 기본값 사용");
            userSeq = "anonymous";
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(userSeq));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(@RequestBody MarkAsReadRequest request) {
        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        if (userSeq == null) {
            log.warn("⚠️ userSeq null. 기본값 사용");
            userSeq = "anonymous";
        }
        notificationService.markAsRead(userSeq, request);
        return ResponseEntity.ok().build();
    }
}
package com.ddiring.Backend_Notification.controller;

import com.ddiring.Backend_Notification.common.util.GatewayRequestHeaderUtils;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        List<String> userSeqs;
        try {
            userSeqs = List.of(GatewayRequestHeaderUtils.getUserSeq());
        } catch (Exception e) {
            userSeqs = List.of("1", "2", "3", "4", "5"); //테스트용
        }

        return notificationService.connectForUsers(userSeqs);
    }

    @GetMapping("/list")
    public ResponseEntity<List<UserNotificationResponse>> getUserNotifications() {
        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        return ResponseEntity.ok(notificationService.getUserNotifications(userSeq));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(@RequestBody MarkAsReadRequest request) {
        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        notificationService.markAsRead(userSeq, request);
        return ResponseEntity.ok().build();
    }

}
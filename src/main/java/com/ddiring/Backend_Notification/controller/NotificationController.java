package com.ddiring.Backend_Notification.controller;

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

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Integer userId) {
        return notificationService.connect(String.valueOf(userId));
    }

    @GetMapping("/list/{userSeq}")
    public ResponseEntity<List<UserNotificationResponse>> getUserNotifications(
            @PathVariable Integer userSeq) {
        List<UserNotificationResponse> responses = notificationService.getUserNotifications(userSeq);
        return ResponseEntity.ok(responses);
    }

//    @PostMapping("/read")
//    public ResponseEntity<Void> markAsRead(
//            @AuthenticationPrincipal CustomUserDetails user,
//            @RequestBody MarkAsReadRequest request) {
//
//        notificationService.markAsRead(user.getUserSeq(), request);
//        return ResponseEntity.ok().build();
//    }
}

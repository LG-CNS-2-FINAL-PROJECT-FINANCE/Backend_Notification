package com.ddiring.Backend_Notification.controller;

import com.ddiring.Backend_Notification.common.exception.ApplicationException;
import com.ddiring.Backend_Notification.common.exception.ErrorCode;
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
    public SseEmitter stream(
            @RequestParam(value = "userSeq", required = false) List<String> userSeqList,
            @RequestParam(value = "token", required = false) String token
    ) {
        if (userSeqList == null || userSeqList.isEmpty()) {
            if (token == null || token.isBlank()) {
                throw new ApplicationException(ErrorCode.UNAUTHORIZED);
            }
            // 토큰 디코딩 후 userSeq 추출
            String userSeq = GatewayRequestHeaderUtils.getUserSeqFromToken(token);
            userSeqList = Collections.singletonList(userSeq);
        }
        return notificationService.connectForUsers(userSeqList);
    }

//    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter stream(@RequestParam(value = "userSeq", required = false) List<String> userSeqList) {
//        if (userSeqList == null || userSeqList.isEmpty()) {
//            String userSeq = GatewayRequestHeaderUtils.getUserSeq();
//            if (userSeq == null || userSeq.isBlank()) {
//                throw new ApplicationException(ErrorCode.UNAUTHORIZED);
//            }
//            userSeqList = Collections.singletonList(userSeq);
//        }
//
//        return notificationService.connectForUsers(userSeqList);
//    }

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
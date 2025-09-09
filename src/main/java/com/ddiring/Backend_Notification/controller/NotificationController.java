package com.ddiring.Backend_Notification.controller;

import com.ddiring.Backend_Notification.dto.response.UserNotificationResponse;
import com.ddiring.Backend_Notification.dto.request.MarkAsReadRequest;
import com.ddiring.Backend_Notification.service.NotificationService;
import com.ddiring.Backend_Notification.common.util.GatewayRequestHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<List<UserNotificationResponse>> getUserNotifications() {
        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
        if (userSeq == null) {
            log.warn("⚠️ userSeq null. 기본값 사용");
            userSeq = "anonymous";
        }
        log.info("🔥 [알림 리스트 조회] userSeq={}", userSeq);
        return ResponseEntity.ok(notificationService.getUserNotifications(userSeq));
    }

    // 읽음 처리
//    @PostMapping("/read")
//    public ResponseEntity<Void> markAsRead(@RequestBody MarkAsReadRequest request) {
//        String userSeq = GatewayRequestHeaderUtils.getUserSeq();
//        if (userSeq == null) {
//            log.warn("⚠️ userSeq null. 기본값 사용");
//            userSeq = "anonymous";
//        }
//        notificationService.markAsRead(userSeq, request);
//        log.info("✅ [알림 읽음 처리 완료] userSeq={}, count={}", userSeq,
//                request.getUserNotificationSeqs().size());
//        return ResponseEntity.ok().build();
//    }
}
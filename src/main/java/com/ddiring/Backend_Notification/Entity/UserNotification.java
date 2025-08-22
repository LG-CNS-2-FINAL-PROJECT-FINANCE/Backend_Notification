package com.ddiring.Backend_Notification.Entity;

import com.ddiring.Backend_Notification.converter.NotificationStatusConverter;
import com.ddiring.Backend_Notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {
    //유저 알림 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_seq", nullable = false)
    private Integer userNotificationSeq;

    //알림 번호
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_seq", nullable = false)
    private Notification notification;

    @Column(name = "user_seq", nullable = false)
    private Integer userSeq;

    //알림 전송 시간
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    //알림 상태
    @Convert(converter = NotificationStatusConverter.class)
    @Column(name = "notification_status", nullable = false)
    private NotificationStatus notificationStatus;

    //알림 읽은 시간
    @Column(name = "read_at", nullable = true)
    private LocalDateTime readAt;

    //생성자
    @Column(name = "created_id", nullable = false, length = 20)
    private String createdId;

    //생성일자
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    //수정자
    @Column(name = "updated_id", nullable = false, length = 20)
    private String updatedId;

    //수정일자
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

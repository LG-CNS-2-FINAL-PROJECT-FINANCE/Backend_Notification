package com.ddiring.Backend_Notification.Entity;

import com.ddiring.Backend_Notification.converter.NotificationTypeConverter;
import com.ddiring.Backend_Notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    //알림 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_seq", nullable = false)
    private Integer notificationSeq;

    //Kafka 이벤트 ID (UUID)
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    //알림 종류
    @Convert(converter = NotificationTypeConverter.class)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    // 알림 내용
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

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
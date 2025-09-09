package com.ddiring.Backend_Notification.Entity;

import com.ddiring.Backend_Notification.converter.DeviceTypeConverter;
import com.ddiring.Backend_Notification.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserDeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userDeviceTokenId;

    @Column(name = "user_seq", nullable = false)
    private String userSeq;

    @Column(name = "device_token", nullable = false)
    private String deviceToken;

    @Convert(converter = DeviceTypeConverter.class)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

package com.ddiring.Backend_Notification.dto.request;

import com.ddiring.Backend_Notification.enums.DeviceType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserDeviceTokenRequest {
    private String userSeq;
    private String fcmToken;
    private DeviceType deviceType;
}

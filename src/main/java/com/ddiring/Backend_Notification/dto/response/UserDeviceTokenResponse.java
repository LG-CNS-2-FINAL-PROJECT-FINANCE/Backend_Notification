package com.ddiring.Backend_Notification.dto.response;

import com.ddiring.Backend_Notification.enums.DeviceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDeviceTokenResponse {
    private Integer userDeviceTokenId;
    private String userSeq;
    private String fcmToken;
    private DeviceType deviceType;
    private String message;
}

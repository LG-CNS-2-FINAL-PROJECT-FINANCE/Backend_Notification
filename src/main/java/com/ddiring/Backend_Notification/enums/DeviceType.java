package com.ddiring.Backend_Notification.enums;

public enum DeviceType {
    WEB(0),         //web
    ANDROID(1),     //android
    IOS(2);         //mac

    private final int code;

    DeviceType(int code) { this.code = code; }

    public int getCode() {
        return code;
    }

    public static DeviceType fromCode(int code) {
        for (DeviceType type : DeviceType.values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Invalid NotificationStatus code: " + code);
    }
}

package com.ddiring.Backend_Notification.enums;

public enum NotificationType {
    INFORMATION(1),     //정보
    WARNING(0),         //경고
    ERROR(-1);          //에러

    private final int code;

    NotificationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static NotificationType fromCode(int code) {
        for (NotificationType type : NotificationType.values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Invalid NotificationType code: " + code);
    }
}
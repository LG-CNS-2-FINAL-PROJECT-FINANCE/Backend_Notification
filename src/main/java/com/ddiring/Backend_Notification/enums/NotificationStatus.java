package com.ddiring.Backend_Notification.enums;

public enum NotificationStatus {
    UNREAD(0),     //안 읽음
    READ(1);       //읽음

    private final int code;

    NotificationStatus(int code) { this.code = code; }

    public int getCode() {
        return code;
    }

    public static NotificationStatus fromCode(int code) {
        for (NotificationStatus status : NotificationStatus.values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("Invalid NotificationStatus code: " + code);
    }
}
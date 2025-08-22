package com.ddiring.Backend_Notification.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class MarkAsReadRequest {
    private List<Integer> userNotificationSeqs;
}

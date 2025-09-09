package com.ddiring.Backend_Notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmRequest {
    private String deviceToken;
    private String title;
    private String body;
}
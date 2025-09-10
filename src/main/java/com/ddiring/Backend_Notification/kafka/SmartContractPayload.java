package com.ddiring.Backend_Notification.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartContractPayload {
    private String projectId;
    private Long investmentId;
    private String investorAddress;
    private Long tokenAmount;
    private String status;

    // 실패 이벤트일 때만 존재
    private String errorType;
    private String errorMessage;
}
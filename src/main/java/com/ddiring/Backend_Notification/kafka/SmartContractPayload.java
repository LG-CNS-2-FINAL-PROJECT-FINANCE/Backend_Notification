package com.ddiring.Backend_Notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 추가: 알 수 없는 필드는 무시
public class SmartContractPayload {
    private String projectId;
    private Long investmentId;
    private Long tradeId;
    private String investorAddress;
    private String buyerAddress;
    private String sellerAddress;
    private Long tokenAmount;
    private Long tradeAmount;
    private String status;

    private String errorType;
    private String errorMessage;

    private String reason;
    private Long initialAmountPerToken;
}

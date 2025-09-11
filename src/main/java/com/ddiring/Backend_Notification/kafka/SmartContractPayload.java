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

    // 실패 이벤트용
    private String errorType;
    private String errorMessage;

    // 추가 가능
    private String reason;
    private Long initialAmountPerToken;
}

package com.ddiring.Backend_Notification.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 파라미터입니다.", HttpStatus.BAD_REQUEST),
    SERVER_ERROR("SERVER_ERROR", "서버 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() { return code; }
    public String defaultMessage() { return defaultMessage; }
    public HttpStatus status() { return httpStatus; }
}
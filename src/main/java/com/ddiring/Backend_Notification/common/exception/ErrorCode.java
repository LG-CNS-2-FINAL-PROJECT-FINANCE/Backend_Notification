package com.ddiring.Backend_Notification.common.exception;

import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUNT", "사용자를 찾을 수 없습니다.", NOT_FOUND),
    INVALID_USER_SEQ("INVALID_USER_SEQ", "잘못된 사용자입니다.", HttpStatus.BAD_REQUEST),
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
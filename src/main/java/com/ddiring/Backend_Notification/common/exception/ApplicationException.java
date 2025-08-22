package com.ddiring.Backend_Notification.common.exception;

import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String overrideMessage;

    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.overrideMessage = null;
    }

    public ApplicationException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage != null ? overrideMessage : errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.overrideMessage = overrideMessage;
    }

    public HttpStatus getStatus() {return errorCode.status(); }
    public String getErrorCode() { return errorCode.code(); }
    public String getErrorMessage() { return overrideMessage != null ? overrideMessage : errorCode.defaultMessage(); }
}
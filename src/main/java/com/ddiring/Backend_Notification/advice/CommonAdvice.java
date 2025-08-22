package com.ddiring.Backend_Notification.advice;

import com.ddiring.Backend_Notification.common.handler.ApiResponse;
import com.ddiring.Backend_Notification.common.exception.ApplicationException;
import com.ddiring.Backend_Notification.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CommonAdvice {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<String>> ApplicationException(ApplicationException ex) {
        log.warn("Application error: code={}, message={}", ex.getErrorCode(), ex.getErrorMessage());
        ApiResponse<String> body = ApiResponse.createError(
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getErrorMessage(),
                null
        );
        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> UnexpectedException(Exception ex) {
        log.error("Unexpected server error", ex);
        ApiResponse<String> body = ApiResponse.createError(
                ErrorCode.SERVER_ERROR.status().value(),
                ErrorCode.SERVER_ERROR.code(),
                ErrorCode.SERVER_ERROR.defaultMessage(),
                null
        );
        return  ResponseEntity.status(500).body(body);
    }
}
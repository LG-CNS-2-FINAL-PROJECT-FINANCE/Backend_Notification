package com.ddiring.Backend_Notification.common.handler;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String code;
    private String message;
    private T data;

    public ApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> createOk(T data) {
        return new ApiResponse<>(200, "OK", "요청을 성공하였습니다.", data);
    }

    public static ApiResponse<String> defaultOk() {
        return ApiResponse.createOk(null);
    }

    public static <T> ApiResponse<T> createError(int status, String code, String message, T data) {
        return new ApiResponse<>(status, code, message, data);
    }
}
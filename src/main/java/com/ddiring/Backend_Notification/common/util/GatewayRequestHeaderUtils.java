package com.ddiring.Backend_Notification.common.util;

import com.ddiring.Backend_Notification.common.exception.ApplicationException;
import com.ddiring.Backend_Notification.common.exception.ErrorCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class GatewayRequestHeaderUtils {
    public static String getRequestHeaderParamAsString(String key) {
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return requestAttributes.getRequest().getHeader(key);
    }

    public static String getUserSeq() {
        String userSeq = getRequestHeaderParamAsString("userSeq");

        if (userSeq == null || userSeq.isBlank()) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }

        return userSeq;
    }
}
package com.ddiring.Backend_Notification.common.util;

import com.ddiring.Backend_Notification.common.exception.ApplicationException;
import com.ddiring.Backend_Notification.common.exception.ErrorCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

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

    // JWT에서 userSeq 추출 (SSE용)
    public static String getUserSeqFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // 토큰에서 서명 없이 Claims 파싱
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody();

            String userSeq = claims.get("userSeq", String.class);
            if (userSeq == null || userSeq.isBlank()) {
                throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
            }

            return userSeq;
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED);
        }
    }
}
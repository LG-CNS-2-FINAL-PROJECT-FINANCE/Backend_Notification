package com.ddiring.Backend_Notification.common.util;

import com.ddiring.Backend_Notification.common.exception.ApplicationException;
import com.ddiring.Backend_Notification.common.exception.ErrorCode;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.nio.charset.StandardCharsets;

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
            // 쿠버네티스 환경에서 JWT_SECRET 환경 변수로 주입
            String secret = System.getenv("JWT_SECRET");
            if (secret == null || secret.isBlank()) {
                throw new ApplicationException(ErrorCode.UNAUTHORIZED);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
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
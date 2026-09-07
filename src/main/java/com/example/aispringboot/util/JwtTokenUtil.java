package com.example.aispringboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.aispringboot.config.JwtConfig;
import com.example.aispringboot.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * JWT 工具：token 生成 / 校验 / 从当前请求读取身份信息。
 */
@Component
public class JwtTokenUtil implements ApplicationContextAware {

    public record TokenVerificationResult(Long userId, String username, Integer roleType, boolean valid) {
    }

    private static final String ISSUER = "mental-health-assistant";
    private static final String HEADER_TOKEN = "token";
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig jwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }

    public static String generateToken(Long userId, String username, Integer roleType) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig().getSecret());
            JWTCreator.Builder builder = JWT.create()
                    .withClaim("userId", userId)
                    .withClaim("username", username)
                    .withClaim("roleType", roleType)
                    .withIssuer(ISSUER)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + jwtConfig().getExpiration()));
            return builder.sign(algorithm);
        } catch (Exception e) {
            throw new BusinessException("生成token失败: " + e.getMessage());
        }
    }

    /** 前端请求头约定的 token 名（未使用 Bearer 前缀）。 */
    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader(HEADER_TOKEN);
        return StringUtils.hasText(token) ? token : null;
    }

    /** 过滤器校验通过后会把 token 放入 request attribute，这里优先读取。 */
    public static String getCurrentToken() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String cached = (String) request.getAttribute("jwtToken");
        return cached != null ? cached : extractTokenFromRequest(request);
    }

    /** 校验并解析 token，非法返回 null。 */
    public static TokenVerificationResult validateToken(String token) {
        DecodedJWT jwt;
        try {
            jwt = verifyToken(token);
        } catch (Exception e) {
            return null;
        }
        Long userId = jwt.getClaim("userId").asLong();
        String username = jwt.getClaim("username").asString();
        Integer roleType = parseRoleType(jwt);
        if (userId != null && StringUtils.hasText(username) && roleType != null) {
            return new TokenVerificationResult(userId, username, roleType, true);
        }
        return null;
    }

    public static Long getCurrentUserId() {
        return verifyToken(getCurrentToken()).getClaim("userId").asLong();
    }

    public static Integer getCurrentRoleType() {
        return parseRoleType(verifyToken(getCurrentToken()));
    }

    public static DecodedJWT verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JWTVerificationException("Token不能为空");
        }
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig().getSecret());
        JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        return verifier.verify(token);
    }

    /** roleType 兼容数字与字符串两种存法。 */
    private static Integer parseRoleType(DecodedJWT jwt) {
        try {
            return jwt.getClaim("roleType").asInt();
        } catch (Exception e) {
            String roleTypeStr = jwt.getClaim("roleType").asString();
            return StringUtils.hasText(roleTypeStr) ? Integer.valueOf(roleTypeStr) : null;
        }
    }
}

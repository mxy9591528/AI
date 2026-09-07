package com.example.aispringboot.util;

import com.example.aispringboot.common.ResultCode;
import com.example.aispringboot.config.SecurityConfig;
import com.example.aispringboot.dto.response.UserLoginResponseDTO;
import com.example.aispringboot.enums.UserStatus;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.service.system.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：校验 token → 加载用户 → 写入 SecurityContext。
 * 异步分发（SSE）默认跳过，公开路径由 shouldNotFilter 放行。
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return SecurityConfig.isPublicPath(request.getRequestURI());
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return super.shouldNotFilterAsyncDispatch();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("{} {}", request.getMethod(), request.getRequestURI());
        }
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            // 未提供凭证属于未认证，返回 401（前端拦截器据此引导重新登录）
            ResponseUtil.writeError(response, ResultCode.UNAUTHORIZED);
            return;
        }
        JwtTokenUtil.TokenVerificationResult verification = JwtTokenUtil.validateToken(token);
        if (verification == null || !verification.valid()) {
            clearContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }
        try {
            UserLoginResponseDTO.UserDetailResponseDTO user = userService.getUserById(verification.userId());
            if (user == null || !UserStatus.NORMAL.getCode().equals(user.getStatus())) {
                clearContext();
                ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(buildAuthentication(verification));
            request.setAttribute("jwtToken", token);
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            // token 合法但用户不存在（如已被删除）的场景
            clearContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
        }
    }

    private static UsernamePasswordAuthenticationToken buildAuthentication(JwtTokenUtil.TokenVerificationResult verification) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + verification.roleType()));
        return new UsernamePasswordAuthenticationToken(verification.username(), null, authorities);
    }

    private static void clearContext() {
        SecurityContextHolder.clearContext();
    }
}

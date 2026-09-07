package com.example.aispringboot.util;

import cn.hutool.json.JSONUtil;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 在过滤器链中直接向客户端写统一 JSON 错误响应。
 */
@Slf4j
public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static void writeError(HttpServletResponse response, ResultCode resultCode) {
        int status = switch (resultCode) {
            case UNAUTHORIZED, TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_BLOCKED, TOKEN_ACCESS_FORBIDDEN ->
                    HttpStatus.UNAUTHORIZED.value();
            case AUTHORIZED_ERROR, ACCESS_UNAUTHORIZED -> HttpStatus.FORBIDDEN.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSONUtil.toJsonStr(Result.error(resultCode.getCode(), resultCode.getMsg(), null)));
            writer.flush();
        } catch (IOException e) {
            log.error("写入错误响应失败: {}", e.getMessage());
        }
    }
}

package com.example.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.common.ResultCode;
import com.example.aispringboot.dto.command.ConsultationSessionCreateDTO;
import com.example.aispringboot.dto.command.ConsultationStreamDTO;
import com.example.aispringboot.dto.response.StreamChatSession;
import com.example.aispringboot.service.consultation.PsychologicalSupportService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * AI 心理对话入口：创建会话 + SSE 流式对话（打字机效果）。
 */
@Slf4j
@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChatController {

    private static final long STREAM_THROTTLE_MILLIS = 50;

    @Resource
    private PsychologicalSupportService psychologicalSupportService;

    @PostMapping("/session/start")
    public Result<StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(psychologicalSupportService.startSession(userId, createDTO));
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        if (userId == null) {
            return errorEvent(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return psychologicalSupportService
                .streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage())
                .map(fragment -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", fragment, "type", "normal"))))
                        .build())
                .concatWith(doneEvent())
                .onErrorResume(e -> {
                    log.error("流式对话失败 sessionId={}", streamDTO.getSessionId(), e);
                    return errorEvent(ResultCode.SYSTEM_ERROR, "AI服务暂时不可用，请稍后重试");
                })
                .delayElements(Duration.ofMillis(STREAM_THROTTLE_MILLIS));
    }

    private static Flux<ServerSentEvent<String>> errorEvent(ResultCode code, String detail) {
        return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(JSONUtil.toJsonStr(Result.error(code.getCode(), code.getMsg(), detail)))
                .build());
    }

    private static Flux<ServerSentEvent<String>> doneEvent() {
        return Flux.just(ServerSentEvent.<String>builder().event("done").data("{}").build());
    }
}

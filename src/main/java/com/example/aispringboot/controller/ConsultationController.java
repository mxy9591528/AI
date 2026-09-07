package com.example.aispringboot.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.response.ConsultationMessageResponseDTO;
import com.example.aispringboot.dto.response.SessionListItemVO;
import com.example.aispringboot.entity.ConsultationSession;
import com.example.aispringboot.exception.BusinessException;
import com.example.aispringboot.service.consultation.ConsultationMessageService;
import com.example.aispringboot.service.consultation.ConsultationSessionService;
import com.example.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 咨询会话管理：会话列表（管理员可见全部）、消息记录、删除、会话情绪分析结果。
 */
@RestController
@RequestMapping("/api/psychological-chat")
public class ConsultationController {

    /** 管理员角色码（与 UserTypeEnum 对应，前端 hasRole('2') 一致）。 */
    private static final int ROLE_ADMIN = 2;
    private static final String SESSION_PREFIX = "session_";

    @Resource
    private ConsultationSessionService consultationSessionService;
    @Resource
    private ConsultationMessageService consultationMessageService;

    @GetMapping("/sessions")
    public Result<Page<SessionListItemVO>> sessions(
            @RequestParam(required = false) Long currentPage,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long pageSize) {
        long page = firstNonNull(currentPage, current, pageNum, 1L);
        long pageSizeVal = firstNonNull(size, pageSize, 10L);
        Long userId = JwtTokenUtil.getCurrentUserId();
        Long queryUserId = JwtTokenUtil.getCurrentRoleType() == ROLE_ADMIN ? null : userId;
        return Result.ok(consultationSessionService.sessionPage(page, pageSizeVal, queryUserId));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ConsultationMessageResponseDTO>> sessionMessages(@PathVariable Long id) {
        checkSessionPermission(consultationSessionService.getSessionById(id));
        return Result.ok(consultationMessageService.listMessagesBySessionId(id));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        checkSessionPermission(consultationSessionService.getSessionById(id));
        consultationSessionService.deleteSession(id);
        return Result.ok();
    }

    @GetMapping("/session/{sessionId}/emotion")
    public Result<Map<String, Object>> sessionEmotion(@PathVariable String sessionId) {
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId == null) {
            throw new BusinessException("会话ID格式错误");
        }
        ConsultationSession session = consultationSessionService.getSessionById(dbSessionId);
        checkSessionPermission(session);
        if (StrUtil.isNotBlank(session.getLastEmotionAnalysis())) {
            return Result.ok(JSONUtil.parseObj(session.getLastEmotionAnalysis()));
        }
        return Result.ok(defaultEmotion());
    }

    /** 兼容前端多种分页参数名，取第一个非空值。 */
    private static long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        throw new IllegalArgumentException("分页参数缺失");
    }

    private static Long parseSessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        String idStr = sessionId.startsWith(SESSION_PREFIX) ? sessionId.substring(SESSION_PREFIX.length()) : sessionId;
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void checkSessionPermission(ConsultationSession session) {
        boolean isAdmin = JwtTokenUtil.getCurrentRoleType() == ROLE_ADMIN;
        if (!isAdmin && !JwtTokenUtil.getCurrentUserId().equals(session.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
    }

    private static Map<String, Object> defaultEmotion() {
        return Map.of(
                "primaryEmotion", "中性",
                "emotionScore", 50,
                "isNegative", false,
                "riskLevel", 0,
                "suggestion", "情绪状态平稳，保持良好的心态哦",
                "riskDescription", "当前情绪状态稳定，无需特别关注",
                "improvementSuggestions", List.of("保持规律作息", "适当运动", "与朋友多交流"));
    }
}

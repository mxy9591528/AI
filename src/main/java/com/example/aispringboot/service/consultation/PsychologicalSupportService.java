package com.example.aispringboot.service.consultation;

import com.example.aispringboot.ai.AiProviderManager;
import com.example.aispringboot.ai.OllamaSseClient;
import com.example.aispringboot.ai.PromptManage;
import com.example.aispringboot.dto.command.ConsultationSessionCreateDTO;
import com.example.aispringboot.dto.response.ConsultationMessageResponseDTO;
import com.example.aispringboot.dto.response.StreamChatSession;
import com.example.aispringboot.entity.ConsultationSession;
import com.example.aispringboot.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PsychologicalSupportService {

    private static final String SESSION_PREFIX = "session_";
    private static final long STREAM_SESSION_TTL_MILLIS = 24 * 60 * 60 * 1000L;

    private final AiProviderManager aiProviderManager;
    private final OllamaSseClient ollamaSseClient;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;

    public PsychologicalSupportService(AiProviderManager aiProviderManager, OllamaSseClient ollamaSseClient,
                                       ConsultationSessionService consultationSessionService,
                                       ConsultationMessageService consultationMessageService) {
        this.aiProviderManager = aiProviderManager;
        this.ollamaSseClient = ollamaSseClient;
        this.consultationSessionService = consultationSessionService;
        this.consultationMessageService = consultationMessageService;
    }

    public StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        ConsultationSession session = consultationSessionService.createSession(userId, createDTO);
        consultationMessageService.saveUserMessage(session.getId(), createDTO.getInitialMessage(), null);
        long now = System.currentTimeMillis();
        return new StreamChatSession(SESSION_PREFIX + session.getId(), userId, createDTO.getInitialMessage(),
                now, now + STREAM_SESSION_TTL_MILLIS, 1, "ACTIVE");
    }

    /**
     * 流式心理疏导对话：用户消息落库 → 记录历史 → 调用当前 AI 供应商流式生成，
     * 全量回复在完成时落库。Ollama 走自定义 SSE 客户端（过滤 reasoning 字段），
     * 百炼走 Spring AI ChatClient。
     */
    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage) {
        return Flux.create(sink -> {
            Long dbSessionId = extractSessionId(sessionId);
            if (dbSessionId == null) {
                sink.error(new BusinessException("会话ID格式错误"));
                return;
            }
            try {
                if (!isInitialMessageAlreadySaved(dbSessionId, userMessage)) {
                    consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
                }
            } catch (Exception e) {
                sink.error(e);
                return;
            }

            // 从数据库加载历史消息（最多 10 条上下文）
            List<String> history = loadHistory(dbSessionId);
            String model = aiProviderManager.currentProvider().equals(AiProviderManager.PROVIDER_OLLAMA)
                    ? aiProviderManager.ollamaModel() : "qwen-plus";

            StringBuilder fullResponse = new StringBuilder();

            Flux<String> fragmentFlux;
            if (AiProviderManager.PROVIDER_OLLAMA.equals(aiProviderManager.currentProvider())) {
                // Ollama：用自定义 SSE 客户端（过滤 qwen3 的 reasoning 字段）
                fragmentFlux = ollamaSseClient.streamChat(model,
                        PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT, history, userMessage);
            } else {
                // 百炼：走 Spring AI ChatClient
                fragmentFlux = aiProviderManager.getChatClient().prompt()
                        .system(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
                        .user(buildMessagesString(history, userMessage))
                        .stream().content();
            }

            fragmentFlux
                    .doOnNext(fragment -> {
                        fullResponse.append(fragment);
                        sink.next(fragment);
                    })
                    .doOnComplete(() -> {
                        consultationMessageService.saveAiMessage(dbSessionId,
                                fullResponse.toString(), aiProviderManager.currentProvider());
                        sink.complete();
                    })
                    .doOnError(e -> {
                        log.error("AI流式对话失败 sessionId={}", sessionId, e);
                        sink.error(e);
                    })
                    .subscribe();
        });
    }

    /** 拼接历史消息为百炼兼容的单条 user message 文本。 */
    private String buildMessagesString(List<String> history, String userMessage) {
        if (history.isEmpty()) return userMessage;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(i % 2 == 0 ? "用户: " : "助手: ").append(history.get(i)).append("\n");
        }
        sb.append("用户: ").append(userMessage);
        return sb.toString();
    }

    /** 从数据库加载该会话最近的消息作为上下文（最多 10 条）。 */
    private List<String> loadHistory(Long dbSessionId) {
        List<ConsultationMessageResponseDTO> all = consultationMessageService.listMessagesBySessionId(dbSessionId);
        int fromIndex = Math.max(0, all.size() - 10);
        List<ConsultationMessageResponseDTO> recent = all.subList(fromIndex, all.size());
        List<String> history = new ArrayList<>();
        for (ConsultationMessageResponseDTO m : recent) {
            if (m.getSenderType() != null && m.getContent() != null) {
                history.add(m.getContent());
            }
        }
        return history;
    }

    /** 首条消息在 startSession 时已落库，避免重复保存。 */
    private boolean isInitialMessageAlreadySaved(Long dbSessionId, String userMessage) {
        if (consultationMessageService.getMessageCountBySessionId(dbSessionId) != 1) {
            return false;
        }
        ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
        return lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent());
    }

    /** 解析 "session_{id}" 格式，非法返回 null（由调用方统一报错）。 */
    public Long extractSessionId(String sessionId) {
        if (sessionId == null || !sessionId.startsWith(SESSION_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(sessionId.substring(SESSION_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

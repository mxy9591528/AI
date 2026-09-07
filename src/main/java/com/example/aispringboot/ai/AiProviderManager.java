package com.example.aispringboot.ai;

import com.example.aispringboot.config.ChatClientConfig;
import com.example.aispringboot.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 供应商管理：维护百炼 / 本地 Ollama 双客户端，支持管理员运行时切换。
 * Ollama 客户端按模型懒重建，切换过程对上层透明。
 */
@Component
public class AiProviderManager {

    public static final String PROVIDER_BAILIAN = "bailian";
    public static final String PROVIDER_OLLAMA = "ollama";
    private static final double OLLAMA_TEMPERATURE = 0.7;

    private final ChatClient bailianClient;
    private final OpenAiApi ollamaOpenAiApi;
    private final ChatMemory chatMemory;
    private volatile ChatClient ollamaClient;
    private volatile String ollamaModel;
    private volatile String currentProvider;

    public AiProviderManager(@Qualifier("open-ai") ChatClient bailianClient,
                             OpenAiApi ollamaOpenAiApi,
                             ChatMemory chatMemory,
                             @Value("${ai.ollama.model}") String ollamaModel,
                             @Value("${ai.active-provider:bailian}") String activeProvider) {
        this.bailianClient = bailianClient;
        this.ollamaOpenAiApi = ollamaOpenAiApi;
        this.chatMemory = chatMemory;
        this.ollamaModel = ollamaModel;
        this.ollamaClient = buildOllamaClient(ollamaModel);
        if (!PROVIDER_BAILIAN.equals(activeProvider) && !PROVIDER_OLLAMA.equals(activeProvider)) {
            throw new IllegalStateException("ai.active-provider 配置无效: " + activeProvider + "，仅支持 bailian / ollama");
        }
        this.currentProvider = activeProvider;
    }

    public ChatClient getChatClient() {
        return PROVIDER_OLLAMA.equals(currentProvider) ? ollamaClient : bailianClient;
    }

    public String currentProvider() {
        return currentProvider;
    }

    public String ollamaModel() {
        return ollamaModel;
    }

    public synchronized void switchTo(String provider, String model) {
        switch (provider) {
            case PROVIDER_BAILIAN -> currentProvider = PROVIDER_BAILIAN;
            case PROVIDER_OLLAMA -> {
                if (model != null && !model.isBlank() && !model.equals(ollamaModel)) {
                    ollamaModel = model;
                    ollamaClient = buildOllamaClient(model);
                }
                currentProvider = PROVIDER_OLLAMA;
            }
            default -> throw new BusinessException("不支持的AI供应商: " + provider + "，仅支持 bailian / ollama");
        }
    }

    private ChatClient buildOllamaClient(String model) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(ollamaOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(OLLAMA_TEMPERATURE)
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(ChatClientConfig.CHAT_SYSTEM_PROMPT)
                .build();
    }
}

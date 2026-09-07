package com.example.aispringboot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    public static final String CHAT_SYSTEM_PROMPT =
            "你是一个专业的心理疏导师，温和耐心，善于倾听，能够提供专业的心理支持和建议";
    private static final int MEMORY_WINDOW_SIZE = 30;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(MEMORY_WINDOW_SIZE).build();
    }

    @Bean("open-ai")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(CHAT_SYSTEM_PROMPT)
                .build();
    }

    @Bean
    public OpenAiApi ollamaOpenAiApi(@Value("${ai.ollama.base-url}") String baseUrl,
                                     @Value("${ai.ollama.api-key}") String apiKey) {
        return OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
    }
}

package com.example.aispringboot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ollama SSE 流式客户端。
 * 直接调用 Ollama 的 OpenAI 兼容端点 (/v1/chat/completions)，
 * 逐行解析 SSE 事件，过滤掉 qwen3 思考模型返回的 reasoning 字段，
 * 只提取 delta.content 文本片段，返回 Flux<String>。
 *
 * 这样绕开了 Spring AI OpenAiChatModel 对 reasoning 字段的 JSON 解析缺陷
 * （reasoning 是 Ollama 特有字段，OpenAI 标准里没有）。
 */
@Slf4j
@Component
public class OllamaSseClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern DATA_PATTERN = Pattern.compile("^data:\\s*(.+)$");
    private static final String DONE_SIGNAL = "[DONE]";

    private final RestClient restClient;
    private final String apiKey;

    public OllamaSseClient(@Value("${ai.ollama.base-url}") String baseUrl,
                           @Value("${ai.ollama.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(5).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/v1")
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * 流式调用 Ollama Chat Completions，逐行返回纯文本 content 片段（不含 reasoning）。
     */
    public reactor.core.publisher.Flux<String> streamChat(String model, String systemPrompt,
                                                          List<String> history, String userMessage) {
        return reactor.core.publisher.Flux.create(sink -> {
            try {
                List<java.util.Map<String, String>> messages = new ArrayList<>();
                if (systemPrompt != null && !systemPrompt.isBlank()) {
                    messages.add(java.util.Map.of("role", "system", "content", systemPrompt));
                }
                // 历史消息
                for (int i = 0; i < history.size(); i += 2) {
                    if (i < history.size()) {
                        messages.add(java.util.Map.of("role", "user", "content", history.get(i)));
                    }
                    if (i + 1 < history.size()) {
                        messages.add(java.util.Map.of("role", "assistant", "content", history.get(i + 1)));
                    }
                }
                messages.add(java.util.Map.of("role", "user", "content", userMessage));

                java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("stream", true);
                body.put("temperature", 0.7);

                StringBuilder response = restClient.post()
                        .uri("/chat/completions")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .exchange((req, resp) -> {
                            StringBuilder sb = new StringBuilder();
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.isBlank()) continue;
                                    Matcher m = DATA_PATTERN.matcher(line);
                                    if (!m.matches()) continue;
                                    String data = m.group(1).trim();
                                    if (DONE_SIGNAL.equals(data)) break;

                                    try {
                                        JsonNode root = MAPPER.readTree(data);
                                        JsonNode choices = root.path("choices");
                                        if (choices.isArray() && !choices.isEmpty()) {
                                            JsonNode delta = choices.get(0).path("delta");
                                            // 只提取 content，忽略 reasoning
                                            String content = delta.path("content").asText("");
                                            if (!content.isBlank()) {
                                                sb.append(content);
                                                sink.next(content);
                                            }
                                        }
                                    } catch (Exception jsonEx) {
                                        log.warn("解析 Ollama SSE chunk 失败: {}", jsonEx.getMessage());
                                    }
                                }
                            }
                            return sb;
                        });
                sink.complete();
            } catch (Exception e) {
                log.error("Ollama 流式调用失败", e);
                sink.error(e);
            }
        });
    }
}

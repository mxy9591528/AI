package com.example.aispringboot.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.aispringboot.ai.AiProviderManager;
import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.command.AiProviderSwitchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 供应商配置（管理员）：查看当前供应商 / Ollama 在线状态与模型列表，运行时切换。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-config")
public class AiProviderController {

    private final AiProviderManager aiProviderManager;

    @Value("${ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @GetMapping("/provider")
    public Result<Map<String, Object>> getProviderStatus() {
        return Result.ok(buildStatus());
    }

    @PutMapping("/provider")
    public Result<Map<String, Object>> switchProvider(@RequestBody AiProviderSwitchDTO dto) {
        aiProviderManager.switchTo(dto.getProvider(), dto.getModel());
        log.info("AI供应商已切换为: {} (ollama模型: {})", aiProviderManager.currentProvider(), aiProviderManager.ollamaModel());
        return Result.ok(buildStatus());
    }

    private Map<String, Object> buildStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentProvider", aiProviderManager.currentProvider());
        status.put("ollamaModel", aiProviderManager.ollamaModel());
        status.put("availableProviders", List.of(AiProviderManager.PROVIDER_BAILIAN, AiProviderManager.PROVIDER_OLLAMA));
        status.put("ollamaOnline", false);
        status.put("ollamaModels", List.of());
        fetchOllamaModels().ifPresentOrElse(models -> {
            status.put("ollamaOnline", true);
            status.put("ollamaModels", models);
        }, () -> log.warn("查询Ollama模型列表失败（可能未启动Ollama）"));
        return status;
    }

    /** 探测 Ollama /api/tags，在线则返回模型名列表。 */
    private java.util.Optional<List<String>> fetchOllamaModels() {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
            factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
            String json = RestClient.builder().baseUrl(ollamaBaseUrl)
                    .requestFactory(factory).build()
                    .get().uri("/api/tags").retrieve().body(String.class);
            JSONArray modelArray = JSONUtil.parseObj(json).getJSONArray("models");
            List<String> models = modelArray.stream()
                    .map(m -> ((JSONObject) m).getStr("name"))
                    .toList();
            return java.util.Optional.of(models);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}

package com.example.llmwiki.api;

import com.example.llmwiki.config.LlmProperties;
import com.example.llmwiki.llm.ChatClient;
import com.example.llmwiki.llm.EmbeddingClient;
import com.example.llmwiki.llm.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 设置接口：读取 / 更新 LLM 配置 + 健康探测。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LlmProperties llmProperties;
    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;

    @GetMapping("/llm")
    public LlmProperties get() {
        return llmProperties;
    }

    @PutMapping("/llm")
    public Map<String, Object> update(@RequestBody LlmProperties newCfg) {
        if (newCfg.getChat() != null) {
            llmProperties.setChat(newCfg.getChat());
        }
        if (newCfg.getEmbedding() != null) {
            llmProperties.setEmbedding(newCfg.getEmbedding());
        }
        if (newCfg.getVision() != null) {
            llmProperties.setVision(newCfg.getVision());
        }
        return Map.of("ok", true);
    }

    @PostMapping("/llm/ping")
    public Map<String, Object> ping() {
        Map<String, Object> r = new java.util.HashMap<>();
        try {
            String msg = chatClient.ping();
            r.put("chat", Map.of("ok", true, "echo", msg.length() > 200 ? msg.substring(0, 200) : msg));
        } catch (LlmException e) {
            r.put("chat", Map.of("ok", false, "error", e.getMessage()));
        }
        try {
            float[] v = embeddingClient.embed("hello");
            r.put("embedding", Map.of("ok", v.length > 0, "dim", v.length));
        } catch (LlmException e) {
            r.put("embedding", Map.of("ok", false, "error", e.getMessage()));
        }
        return r;
    }
}

package com.example.llmwiki.llm;

import com.example.llmwiki.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * OpenAI 兼容的 Chat Completions 客户端。
 * <p>
 * 所有遵循 OpenAI Chat 协议的厂商（DeepSeek / Kimi / 通义 / 智谱 / Ollama / vLLM 等）皆可适配。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClient {

    private final LlmProperties llmProperties;
    private final RestClient sharedRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 单轮 system + user 调用。
     */
    public String complete(String systemPrompt, String userPrompt) {
        return complete(List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        ));
    }

    /**
     * 多轮调用。
     *
     * @param messages role / content 序列
     * @return assistant 文本
     */
    public String complete(List<Message> messages) {
        LlmProperties.Chat chat = llmProperties.getChat();
        if (chat.getApiKey() == null || chat.getApiKey().isBlank()) {
            throw new LlmException("Chat API Key 未配置，请前往 Settings 页填入");
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", chat.getModel());
        root.put("temperature", chat.getTemperature());
        // 输出上限，避免长输出被默认 max_tokens 截断导致 JSON 不完整
        if (chat.getMaxTokens() != null && chat.getMaxTokens() > 0) {
            root.put("max_tokens", chat.getMaxTokens());
        }
        // 如果 system prompt 明确要求 JSON，则开启 json_object 强约束
        boolean wantsJson = Boolean.TRUE.equals(chat.getJsonMode())
                && messages.stream().anyMatch(m -> "system".equals(m.role())
                        && m.content() != null && m.content().toUpperCase().contains("JSON"));
        if (wantsJson) {
            ObjectNode rf = root.putObject("response_format");
            rf.put("type", "json_object");
        }
        ArrayNode arr = root.putArray("messages");
        for (Message m : messages) {
            ObjectNode n = arr.addObject();
            n.put("role", m.role());
            n.put("content", m.content());
        }

        String url = trimSlash(chat.getBaseUrl()) + "/chat/completions";
        try {
            JsonNode resp = sharedRestClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + chat.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(root))
                    .retrieve()
                    .body(JsonNode.class);

            if (resp == null || !resp.has("choices") || resp.get("choices").isEmpty()) {
                throw new LlmException("LLM 返回为空: " + resp);
            }
            JsonNode choice = resp.get("choices").get(0);
            String finish = choice.path("finish_reason").asText("");
            String text = choice.path("message").path("content").asText();
            if ("length".equalsIgnoreCase(finish)) {
                log.warn("Chat 响应被 max_tokens 截断，model={} maxTokens={}", chat.getModel(), chat.getMaxTokens());
            }
            return text;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chat 调用失败 url={} model={}", url, chat.getModel(), e);
            throw new LlmException("Chat 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 探测连通性（极小 prompt）。
     */
    public String ping() {
        return complete("You are a healthcheck.", "ping");
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * 一条消息。
     */
    public record Message(String role, String content) {
    }
}

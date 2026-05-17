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

import java.util.Base64;

/**
 * OpenAI 兼容 Vision 客户端：对单张图片调用多模态 LLM 生成事实性描述。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionClient {

    private final LlmProperties llmProperties;
    private final RestClient sharedRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 是否启用 Vision。
     */
    public boolean isEnabled() {
        LlmProperties.Vision v = llmProperties.getVision();
        return Boolean.TRUE.equals(v.getEnabled())
                && v.getApiKey() != null && !v.getApiKey().isBlank();
    }

    /**
     * 给图片打 caption。
     *
     * @param imageBytes 图片字节
     * @param mime       MIME 类型
     * @return 事实性描述（失败返回空串）
     */
    public String caption(byte[] imageBytes, String mime) {
        if (!isEnabled()) {
            return "";
        }
        LlmProperties.Vision v = llmProperties.getVision();
        try {
            String dataUrl = "data:" + (mime == null ? "image/png" : mime) + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);

            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", v.getModel());
            ArrayNode messages = root.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            ArrayNode contentArr = userMsg.putArray("content");
            ObjectNode textPart = contentArr.addObject();
            textPart.put("type", "text");
            textPart.put("text", "请用一段事实性中文描述本图，包含可见的对象、文字、关键数字与图表类型。不要主观解读。");
            ObjectNode imgPart = contentArr.addObject();
            imgPart.put("type", "image_url");
            imgPart.putObject("image_url").put("url", dataUrl);

            String url = trimSlash(v.getBaseUrl()) + "/chat/completions";
            JsonNode resp = sharedRestClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + v.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(root))
                    .retrieve()
                    .body(JsonNode.class);

            if (resp == null || !resp.has("choices") || resp.get("choices").isEmpty()) {
                return "";
            }
            return resp.get("choices").get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("Vision 调用失败，跳过 caption: {}", e.getMessage());
            return "";
        }
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}

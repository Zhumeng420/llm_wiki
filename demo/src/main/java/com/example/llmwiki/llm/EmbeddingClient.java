package com.example.llmwiki.llm;

import com.example.llmwiki.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容 Embedding 客户端。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final LlmProperties llmProperties;
    private final RestClient sharedRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 嵌入单条文本，返回 float[]。
     */
    public float[] embed(String text) {
        List<float[]> all = embed(List.of(text));
        return all.isEmpty() ? new float[0] : all.get(0);
    }

    /**
     * 批量嵌入。
     */
    public List<float[]> embed(List<String> texts) {
        LlmProperties.Embedding emb = llmProperties.getEmbedding();
        if (emb.getApiKey() == null || emb.getApiKey().isBlank()) {
            throw new LlmException("Embedding API Key 未配置");
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", emb.getModel());
        root.putArray("input").addAll(
                texts.stream().map(t -> objectMapper.getNodeFactory().textNode(t)).toList()
        );

        String url = trimSlash(emb.getBaseUrl()) + "/embeddings";
        try {
            JsonNode resp = sharedRestClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + emb.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(root))
                    .retrieve()
                    .body(JsonNode.class);
            List<float[]> out = new ArrayList<>();
            if (resp == null || !resp.has("data")) {
                throw new LlmException("Embedding 返回异常: " + resp);
            }
            for (JsonNode item : resp.get("data")) {
                JsonNode vec = item.get("embedding");
                float[] arr = new float[vec.size()];
                for (int i = 0; i < vec.size(); i++) {
                    arr[i] = (float) vec.get(i).asDouble();
                }
                out.add(arr);
            }
            return out;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding 调用失败 url={} model={}", url, emb.getModel(), e);
            throw new LlmException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}

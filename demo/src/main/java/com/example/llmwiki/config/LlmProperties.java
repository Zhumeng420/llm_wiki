package com.example.llmwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 模型相关配置（Chat / Embedding / Vision），全部走 OpenAI 兼容协议。
 * <p>
 * 通过 {@code llm-wiki.llm.*} 前缀绑定，支持运行时通过 SettingsController 热更新。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm-wiki.llm")
public class LlmProperties {

    /** Chat 模型配置 */
    private Chat chat = new Chat();

    /** Embedding 模型配置 */
    private Embedding embedding = new Embedding();

    /** Vision 模型配置（可选，用于图片理解） */
    private Vision vision = new Vision();

    @Data
    public static class Chat {
        /** OpenAI 兼容的 base URL，如 https://api.openai.com/v1 */
        private String baseUrl = "https://api.openai.com/v1";
        /** API Key */
        private String apiKey = "";
        /** 模型名称 */
        private String model = "gpt-4o-mini";
        /** 采样温度 */
        private Double temperature = 0.2;
        /** 单次输出 token 上限（避免 JSON 被截断），默认 8192 */
        private Integer maxTokens = 8192;
        /** 是否强制 JSON 模式（部分厂商不支持时可关闭） */
        private Boolean jsonMode = true;
        /** 超时（秒） */
        private Integer timeoutSeconds = 120;
    }

    @Data
    public static class Embedding {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "text-embedding-3-small";
        /** 向量维度，需与远端模型保持一致 */
        private Integer dimensions = 1536;
        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Vision {
        private Boolean enabled = false;
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private Integer timeoutSeconds = 120;
    }
}

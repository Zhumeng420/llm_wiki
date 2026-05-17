package com.example.llmwiki.parser.impl;

import com.example.llmwiki.config.ParserProperties;
import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.ParserException;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 飞书文档解析器（OpenAPI v1）。
 * <p>
 * 流程：app_id+app_secret → tenant_access_token → 调用 docx 内容接口；
 * 用户在 Settings 中开启 Feishu 并填入凭证后才生效。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(70)
@Component
@RequiredArgsConstructor
public class FeishuDocParser implements SourceParser {

    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String DOC_RAW_URL_TPL = "https://open.feishu.cn/open-apis/docx/v1/documents/%s/raw_content";

    private final ParserProperties parserProperties;
    private final RestClient sharedRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String kind() {
        return "FEISHU";
    }

    @Override
    public boolean supports(ParseRequest req) {
        return "FEISHU".equalsIgnoreCase(req.getKind());
    }

    @Override
    public RawDocument parse(ParseRequest req) {
        ParserProperties.Feishu cfg = parserProperties.getFeishu();
        if (!Boolean.TRUE.equals(cfg.getEnabled())
                || cfg.getAppId() == null || cfg.getAppId().isBlank()) {
            throw new ParserException("飞书未启用或未配置 app_id/app_secret");
        }
        try {
            String token = fetchTenantToken(cfg.getAppId(), cfg.getAppSecret());
            String docToken = req.getRef();
            String url = String.format(DOC_RAW_URL_TPL, docToken);
            JsonNode resp = sharedRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null || !resp.has("data")) {
                throw new ParserException("飞书返回异常: " + resp);
            }
            String content = resp.get("data").path("content").asText("");
            String text = TextUtils.normalizeWhitespace(content);
            return RawDocument.builder()
                    .sourceKind("FEISHU")
                    .sourceRef(docToken)
                    .displayName(req.getDisplayName() == null ? "feishu:" + docToken : req.getDisplayName())
                    .text(text)
                    .contentHash(TextUtils.sha256(text))
                    .build();
        } catch (Exception e) {
            throw new ParserException("飞书文档解析失败: " + e.getMessage(), e);
        }
    }

    private String fetchTenantToken(String appId, String appSecret) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);
        JsonNode resp = sharedRestClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(JsonNode.class);
        if (resp == null || !resp.has("tenant_access_token")) {
            throw new ParserException("获取飞书 token 失败: " + resp);
        }
        return resp.get("tenant_access_token").asText();
    }
}

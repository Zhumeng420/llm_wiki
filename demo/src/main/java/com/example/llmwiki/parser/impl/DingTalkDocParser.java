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
 * 钉钉文档解析器（钉钉智能填表/Wiki OpenAPI）。
 * <p>
 * 简化实现：appKey+appSecret → access_token → /v1.0/doc/documents/{docId}。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(80)
@Component
@RequiredArgsConstructor
public class DingTalkDocParser implements SourceParser {

    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";
    private static final String DOC_URL_TPL = "https://api.dingtalk.com/v1.0/doc/documents/%s";

    private final ParserProperties parserProperties;
    private final RestClient sharedRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String kind() {
        return "DINGTALK";
    }

    @Override
    public boolean supports(ParseRequest req) {
        return "DINGTALK".equalsIgnoreCase(req.getKind());
    }

    @Override
    public RawDocument parse(ParseRequest req) {
        ParserProperties.DingTalk cfg = parserProperties.getDingtalk();
        if (!Boolean.TRUE.equals(cfg.getEnabled())
                || cfg.getAppKey() == null || cfg.getAppKey().isBlank()) {
            throw new ParserException("钉钉未启用或未配置 app_key/app_secret");
        }
        try {
            String token = fetchAccessToken(cfg.getAppKey(), cfg.getAppSecret());
            String docId = req.getRef();
            String url = String.format(DOC_URL_TPL, docId);
            JsonNode resp = sharedRestClient.get()
                    .uri(url)
                    .header("x-acs-dingtalk-access-token", token)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) {
                throw new ParserException("钉钉返回为空");
            }
            String title = resp.path("title").asText("dingtalk:" + docId);
            String content = resp.path("content").asText("");
            String text = "# " + title + "\n\n" + TextUtils.normalizeWhitespace(content);
            return RawDocument.builder()
                    .sourceKind("DINGTALK")
                    .sourceRef(docId)
                    .displayName(title)
                    .text(text)
                    .contentHash(TextUtils.sha256(text))
                    .build();
        } catch (Exception e) {
            throw new ParserException("钉钉文档解析失败: " + e.getMessage(), e);
        }
    }

    private String fetchAccessToken(String appKey, String appSecret) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("appKey", appKey);
        body.put("appSecret", appSecret);
        JsonNode resp = sharedRestClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(JsonNode.class);
        if (resp == null || !resp.has("accessToken")) {
            throw new ParserException("获取钉钉 token 失败: " + resp);
        }
        return resp.get("accessToken").asText();
    }
}

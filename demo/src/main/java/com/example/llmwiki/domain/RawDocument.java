package com.example.llmwiki.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准化的"原始文档"DTO，所有解析器统一输出此结构供摄入流水线消费。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Builder
public class RawDocument {

    /** 来源类型：FILE / URL / FEISHU / DINGTALK */
    private String sourceKind;

    /** 来源标识（文件名/URL/文档 token） */
    private String sourceRef;

    /** 来源原文（用于回链展示） */
    private String displayName;

    /** 文本正文 */
    private String text;

    /** 内容指纹（SHA256），用于增量缓存 */
    private String contentHash;

    /** 文档语言（zh / en / auto） */
    private String language;

    /** 嵌入图像描述（OCR + Vision 生成） */
    @Builder.Default
    private List<String> imageCaptions = new ArrayList<>();

    /** 元信息（mime/作者/页数/抓取时间等） */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /** 抓取时间 */
    @Builder.Default
    private Instant fetchedAt = Instant.now();
}

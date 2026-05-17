package com.example.llmwiki.parser;

import com.example.llmwiki.domain.RawDocument;

/**
 * 多源解析器统一接口。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
public interface SourceParser {

    /** 解析器类型，与 {@link com.example.llmwiki.domain.SourceRecord#getKind()} 对应或为 MIME 标签 */
    String kind();

    /** 是否能处理该来源 */
    boolean supports(ParseRequest request);

    /** 执行解析 */
    RawDocument parse(ParseRequest request) throws Exception;
}

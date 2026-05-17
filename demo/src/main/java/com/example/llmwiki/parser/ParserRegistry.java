package com.example.llmwiki.parser;

import com.example.llmwiki.domain.RawDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析器注册表，按顺序选择第一个 supports 的实现。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParserRegistry {

    /** 全部解析器（Spring 自动注入） */
    private final List<SourceParser> parsers;

    /**
     * 根据请求匹配并执行解析。
     */
    public RawDocument parse(ParseRequest request) throws Exception {
        for (SourceParser p : parsers) {
            if (p.supports(request)) {
                log.info("使用解析器 {} 处理 ref={}", p.kind(), request.getRef());
                return p.parse(request);
            }
        }
        throw new ParserException("找不到匹配的解析器: kind=" + request.getKind() + ", ref=" + request.getRef());
    }
}

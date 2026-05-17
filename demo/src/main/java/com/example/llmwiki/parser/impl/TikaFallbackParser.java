package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 兜底解析器：用 Apache Tika 处理 txt/md/html/csv 等其它文本类格式。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(1000)
@Component
public class TikaFallbackParser implements SourceParser {

    private final Tika tika = new Tika();

    @Override
    public String kind() {
        return "FILE/FALLBACK";
    }

    @Override
    public boolean supports(ParseRequest req) {
        return "FILE".equalsIgnoreCase(req.getKind());
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        String text = tika.parseToString(new ByteArrayInputStream(req.getFileBytes()));
        return RawDocument.builder()
                .sourceKind("FILE")
                .sourceRef(req.getRef())
                .displayName(req.getDisplayName())
                .text(TextUtils.normalizeWhitespace(text))
                .contentHash(TextUtils.sha256(text))
                .build();
    }
}

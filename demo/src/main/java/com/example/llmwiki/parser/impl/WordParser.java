package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * Word 解析器：支持 .doc 与 .docx。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(20)
@Component
public class WordParser implements SourceParser {

    @Override
    public String kind() {
        return "FILE/WORD";
    }

    @Override
    public boolean supports(ParseRequest req) {
        if (!"FILE".equalsIgnoreCase(req.getKind())) {
            return false;
        }
        String name = (req.getDisplayName() == null ? req.getRef() : req.getDisplayName()).toLowerCase(Locale.ROOT);
        return name.endsWith(".doc") || name.endsWith(".docx");
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        String name = req.getDisplayName() == null ? req.getRef() : req.getDisplayName();
        String text;
        if (name.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(req.getFileBytes()));
                 XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                text = ex.getText();
            }
        } else {
            try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(req.getFileBytes()));
                 WordExtractor ex = new WordExtractor(doc)) {
                text = ex.getText();
            }
        }
        return RawDocument.builder()
                .sourceKind("FILE")
                .sourceRef(req.getRef())
                .displayName(req.getDisplayName())
                .text(TextUtils.normalizeWhitespace(text))
                .contentHash(TextUtils.sha256(text))
                .build();
    }
}

package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * PowerPoint 解析器（ppt / pptx）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(40)
@Component
public class PptParser implements SourceParser {

    @Override
    public String kind() {
        return "FILE/PPT";
    }

    @Override
    public boolean supports(ParseRequest req) {
        if (!"FILE".equalsIgnoreCase(req.getKind())) {
            return false;
        }
        String name = (req.getDisplayName() == null ? req.getRef() : req.getDisplayName()).toLowerCase(Locale.ROOT);
        return name.endsWith(".ppt") || name.endsWith(".pptx");
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        StringBuilder sb = new StringBuilder();
        String name = (req.getDisplayName() == null ? req.getRef() : req.getDisplayName()).toLowerCase(Locale.ROOT);
        if (name.endsWith(".pptx")) {
            try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(req.getFileBytes()))) {
                int idx = 0;
                for (XSLFSlide slide : ppt.getSlides()) {
                    sb.append("\n## Slide ").append(++idx).append('\n');
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape ts) {
                            sb.append(ts.getText()).append('\n');
                        }
                    }
                }
            }
        } else {
            try (HSLFSlideShow ppt = new HSLFSlideShow(new ByteArrayInputStream(req.getFileBytes()))) {
                int idx = 0;
                for (HSLFSlide slide : ppt.getSlides()) {
                    sb.append("\n## Slide ").append(++idx).append('\n');
                    for (java.util.List<HSLFTextParagraph> paragraphs : slide.getTextParagraphs()) {
                        sb.append(HSLFTextParagraph.getText(paragraphs)).append('\n');
                    }
                }
            }
        }
        String text = sb.toString();
        return RawDocument.builder()
                .sourceKind("FILE")
                .sourceRef(req.getRef())
                .displayName(req.getDisplayName())
                .text(text)
                .contentHash(TextUtils.sha256(text))
                .build();
    }
}

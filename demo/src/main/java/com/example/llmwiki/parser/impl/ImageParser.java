package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.llm.VisionClient;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 图片解析器：调用 Vision LLM 生成事实性 caption；若 Vision 未启用则记录元信息为主。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(50)
@Component
@RequiredArgsConstructor
public class ImageParser implements SourceParser {

    private static final List<String> EXTS = List.of(".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif");

    private final VisionClient visionClient;

    @Override
    public String kind() {
        return "FILE/IMAGE";
    }

    @Override
    public boolean supports(ParseRequest req) {
        if (!"FILE".equalsIgnoreCase(req.getKind())) {
            return false;
        }
        String name = (req.getDisplayName() == null ? req.getRef() : req.getDisplayName()).toLowerCase(Locale.ROOT);
        return EXTS.stream().anyMatch(name::endsWith);
    }

    @Override
    public RawDocument parse(ParseRequest req) {
        List<String> captions = new ArrayList<>();
        String text;
        if (visionClient.isEnabled()) {
            String c = visionClient.caption(req.getFileBytes(), req.getMime());
            if (c != null && !c.isBlank()) {
                captions.add(c);
            }
            text = "（图片）" + req.getDisplayName() + "\n\n" + String.join("\n", captions);
        } else {
            text = "（图片，未启用 Vision，仅记录元信息）文件名: " + req.getDisplayName();
            log.info("Vision 未启用，图片仅作元信息收录: {}", req.getDisplayName());
        }
        return RawDocument.builder()
                .sourceKind("FILE")
                .sourceRef(req.getRef())
                .displayName(req.getDisplayName())
                .text(text)
                .imageCaptions(captions)
                .contentHash(TextUtils.sha256(text))
                .build();
    }
}

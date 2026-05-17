package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.llm.VisionClient;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

/**
 * PDF 解析器：基于 PDFBox 抽取文本 + 嵌入图，可选地用 Vision LLM 给图片打 caption。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(10)
@Component
@RequiredArgsConstructor
public class PdfParser implements SourceParser {

    private final VisionClient visionClient;

    @Override
    public String kind() {
        return "FILE/PDF";
    }

    @Override
    public boolean supports(ParseRequest req) {
        if (!"FILE".equalsIgnoreCase(req.getKind())) {
            return false;
        }
        String name = req.getDisplayName() == null ? req.getRef() : req.getDisplayName();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        try (PDDocument doc = Loader.loadPDF(req.getFileBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);

            List<String> captions = new ArrayList<>();
            if (visionClient.isEnabled()) {
                captions.addAll(extractAndCaption(doc));
            }

            return RawDocument.builder()
                    .sourceKind("FILE")
                    .sourceRef(req.getRef())
                    .displayName(req.getDisplayName())
                    .text(TextUtils.normalizeWhitespace(text))
                    .imageCaptions(captions)
                    .contentHash(TextUtils.sha256(text + String.join("|", captions)))
                    .build();
        }
    }

    private List<String> extractAndCaption(PDDocument doc) {
        List<String> captions = new ArrayList<>();
        try {
            PDPageTree pages = doc.getPages();
            int idx = 0;
            int max = Math.min(pages.getCount(), 20); // 控制成本：最多 20 页
            for (int i = 0; i < max; i++) {
                PDPage page = pages.get(i);
                PDResources res = page.getResources();
                if (res == null) {
                    continue;
                }
                for (var name : res.getXObjectNames()) {
                    PDXObject obj = res.getXObject(name);
                    if (obj instanceof PDImageXObject img) {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(img.getImage(), "png", baos);
                            String c = visionClient.caption(baos.toByteArray(), "image/png");
                            if (c != null && !c.isBlank()) {
                                captions.add("[第" + (i + 1) + "页 图" + (++idx) + "] " + c);
                            }
                        } catch (Exception e) {
                            log.debug("图片 caption 失败: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取 PDF 图片失败: {}", e.getMessage());
        }
        return captions;
    }
}

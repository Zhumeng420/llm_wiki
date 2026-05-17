package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 网页解析器：Jsoup 抓取 + Readability4J 主体抽取。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(60)
@Component
public class WebParser implements SourceParser {

    @Override
    public String kind() {
        return "URL";
    }

    @Override
    public boolean supports(ParseRequest req) {
        return "URL".equalsIgnoreCase(req.getKind());
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        String url = req.getRef();
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 LLM-Wiki Crawler")
                .timeout(20_000)
                .followRedirects(true)
                .get();
        Readability4J r4j = new Readability4J(url, doc);
        Article article = r4j.parse();

        String title = article.getTitle() != null ? article.getTitle() : doc.title();
        String body = article.getTextContent() != null && !article.getTextContent().isBlank()
                ? article.getTextContent()
                : doc.body().text();

        Map<String, String> meta = new HashMap<>();
        meta.put("url", url);
        meta.put("title", title == null ? "" : title);

        String text = (title == null ? "" : "# " + title + "\n\n") + TextUtils.normalizeWhitespace(body);
        return RawDocument.builder()
                .sourceKind("URL")
                .sourceRef(url)
                .displayName(title == null ? url : title)
                .text(text)
                .contentHash(TextUtils.sha256(text))
                .metadata(meta)
                .build();
    }
}

package com.example.llmwiki.ingest;

import com.example.llmwiki.config.StorageProperties;
import com.example.llmwiki.domain.WikiPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 WikiPage 写出为 Markdown（含 YAML frontmatter）落到 wikiDir。
 * <p>
 * 兼容 Obsidian Vault：每个 type 一个子目录。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiFileWriter {

    private final StorageProperties storageProperties;

    public File writePage(WikiPage page) {
        String type = page.getType() == null ? "misc" : page.getType();
        File dir = new File(storageProperties.getWikiDir(), type);
        dir.mkdirs();
        File file = new File(dir, page.getSlug() + ".md");
        try {
            Files.writeString(file.toPath(), buildMarkdown(page), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("写出 wiki 文件失败 slug={} : {}", page.getSlug(), e.getMessage());
        }
        return file;
    }

    /**
     * 增量追加一行到 log.md。
     */
    public void appendLog(String line) {
        File logFile = new File(storageProperties.getWikiDir(), "log.md");
        logFile.getParentFile().mkdirs();
        try {
            String entry = "- [" + Instant.now() + "] " + line + "\n";
            Files.writeString(logFile.toPath(), entry, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.debug("写 log 失败: {}", e.getMessage());
        }
    }

    public String buildMarkdown(WikiPage page) {
        List<String> lines = new ArrayList<>();
        lines.add("---");
        lines.add("title: " + safeYaml(page.getTitle()));
        lines.add("slug: " + safeYaml(page.getSlug()));
        lines.add("type: " + safeYaml(page.getType()));
        if (page.getSummary() != null) {
            lines.add("summary: " + safeYaml(page.getSummary()));
        }
        lines.add("sources: [" + page.getSources() + "]");
        lines.add("tags: [" + (page.getTags() == null ? "" : page.getTags()) + "]");
        lines.add("updated_at: " + Instant.now());
        lines.add("---");
        lines.add("");
        lines.add("# " + (page.getTitle() == null ? page.getSlug() : page.getTitle()));
        lines.add("");
        if (page.getContent() != null) {
            lines.add(page.getContent());
        }
        return String.join("\n", lines);
    }

    private String safeYaml(String s) {
        if (s == null) {
            return "";
        }
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}

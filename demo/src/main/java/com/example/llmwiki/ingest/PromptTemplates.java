package com.example.llmwiki.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 模板加载器（缓存到内存，支持 {{var}} 占位）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
public class PromptTemplates {

    private final Map<String, String> cache = new HashMap<>();

    public String render(String template, Map<String, String> vars) {
        String tpl = load(template);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            tpl = tpl.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return tpl;
    }

    private String load(String name) {
        return cache.computeIfAbsent(name, key -> {
            try {
                ClassPathResource res = new ClassPathResource("prompts/" + key + ".md");
                return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("加载 prompt 失败: " + key, e);
            }
        });
    }
}

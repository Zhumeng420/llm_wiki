package com.example.llmwiki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Step2 LLM 生成的 Wiki 页面草稿。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageDraft {

    /** 类型：entity / concept / source / ... */
    private String type;

    /** 标题 */
    private String title;

    /** slug，文件名 */
    private String slug;

    /** 摘要 */
    private String summary;

    /** Markdown 正文（不含 frontmatter） */
    private String body;

    /** [[wikilink]] 目标 slug 列表 */
    @Builder.Default
    private List<String> outLinks = new ArrayList<>();

    /** 标签 */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /** 来源 ref */
    @Builder.Default
    private List<String> sources = new ArrayList<>();
}

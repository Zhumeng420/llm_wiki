package com.example.llmwiki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 在 Step1 / Step2 之间传递的"分析结果"。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    /** 关键实体 */
    @Builder.Default
    private List<String> entities = new ArrayList<>();

    /** 关键概念 */
    @Builder.Default
    private List<String> concepts = new ArrayList<>();

    /** 与已有 wiki 的关联 slug */
    @Builder.Default
    private List<String> connections = new ArrayList<>();

    /** 矛盾点 */
    @Builder.Default
    private List<String> contradictions = new ArrayList<>();

    /** 摘要 */
    private String summary;

    /** 推荐结构（page 类型 + 标题） */
    @Builder.Default
    private List<PageOutline> recommended = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageOutline {
        private String type;
        private String title;
        private String slug;
    }
}

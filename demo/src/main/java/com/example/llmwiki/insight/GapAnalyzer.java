package com.example.llmwiki.insight;

import com.example.llmwiki.domain.WikiPage;
import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.ingest.PromptTemplates;
import com.example.llmwiki.llm.ChatClient;
import com.example.llmwiki.repository.WikiPageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识空白反推：结合结构信号（孤立节点、稀疏社区、桥节点缺失）和语义信号（LLM 审计）。
 * <p>
 * 输出列表给前端 InsightView 展示，并提示用户补充哪些类型的文档。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GapAnalyzer {

    private static final String SYSTEM = "You are a knowledge base auditor. Always respond in 中文.";

    private final WikiPageRepository wikiRepo;
    private final GraphService graphService;
    private final ChatClient chatClient;
    private final PromptTemplates prompts;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 入口：返回综合 gap 报告。
     *
     * @param useLlm 是否调用 LLM 进行语义审计（无 key 时可关闭）
     */
    public GapReport analyze(boolean useLlm) {
        GapReport report = new GapReport();

        // 1) 结构信号
        report.setIsolatedNodes(graphService.isolatedNodes());
        report.setBridgeNodes(graphService.bridgeNodes());
        report.setSparseCommunities(sparseCommunities());

        // 2) 语义信号
        if (useLlm) {
            try {
                LlmGap llm = analyzeBySemantic();
                report.setUnanswered(llm.unanswered);
                report.setMissingTopics(llm.missingTopics);
            } catch (Exception e) {
                log.warn("LLM gap 分析失败: {}", e.getMessage());
                report.setLlmError(e.getMessage());
            }
        }

        // 3) 通用建议
        report.setSuggestions(generalSuggestions(report));
        return report;
    }

    /**
     * 找出包含节点数 <=3 的小社区，视为知识稀疏点。
     */
    private List<SparseCommunity> sparseCommunities() {
        Map<Integer, List<String>> grouped = new HashMap<>();
        for (Map.Entry<String, Integer> e : graphService.community().entrySet()) {
            grouped.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<SparseCommunity> out = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : grouped.entrySet()) {
            if (e.getValue().size() <= 3) {
                SparseCommunity sc = new SparseCommunity();
                sc.communityId = e.getKey();
                sc.members = e.getValue();
                out.add(sc);
            }
        }
        return out;
    }

    /**
     * 调用 LLM，结合 overview 与 purpose 文本反推未答问题。
     */
    private LlmGap analyzeBySemantic() {
        String overview = readPageContent("overview").orElse("(暂无 overview)");
        String purpose = readPageContent("purpose").orElse("(暂无 purpose)");

        Map<String, String> vars = new HashMap<>();
        vars.put("overview", truncate(overview, 4000));
        vars.put("purpose", truncate(purpose, 4000));
        String userPrompt = prompts.render("gap", vars);
        String resp = chatClient.complete(SYSTEM, userPrompt);

        LlmGap gap = new LlmGap();
        try {
            JsonNode root = objectMapper.readTree(stripJson(resp));
            if (root.has("unanswered")) {
                for (JsonNode n : root.get("unanswered")) {
                    Unanswered u = new Unanswered();
                    u.question = n.path("question").asText();
                    u.reason = n.path("reason").asText();
                    if (n.has("suggested_sources")) {
                        u.suggestedSources = new ArrayList<>();
                        for (JsonNode s : n.get("suggested_sources")) {
                            u.suggestedSources.add(s.asText());
                        }
                    }
                    gap.unanswered.add(u);
                }
            }
            if (root.has("missing_topics")) {
                for (JsonNode n : root.get("missing_topics")) {
                    gap.missingTopics.add(n.asText());
                }
            }
        } catch (Exception e) {
            log.warn("解析 gap LLM 输出失败: {}", e.getMessage());
        }
        return gap;
    }

    private List<String> generalSuggestions(GapReport report) {
        List<String> out = new ArrayList<>();
        if (!report.isolatedNodes.isEmpty()) {
            out.add("发现 " + report.isolatedNodes.size() + " 个孤立节点，建议补充能与其建立关联的资料（如行业背景、对比文献）。");
        }
        if (!report.sparseCommunities.isEmpty()) {
            out.add("存在 " + report.sparseCommunities.size() + " 个稀疏社区（成员 ≤ 3），可针对相关主题继续导入更多资料。");
        }
        if (!report.bridgeNodes.isEmpty()) {
            out.add("以下节点连接多个领域，是潜在的核心枢纽：" + String.join("、", report.bridgeNodes));
        }
        if (graphService.totalEdges() == 0 && !graphService.nodes().isEmpty()) {
            out.add("当前节点之间尚未建立有效关联，建议增加跨主题文档以促进图谱连通。");
        }
        if (out.isEmpty()) {
            out.add("当前知识库结构良好，可继续按规划导入新主题资料。");
        }
        return out;
    }

    private Optional<String> readPageContent(String type) {
        List<WikiPage> pages = wikiRepo.findByType(type);
        if (pages.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        for (WikiPage p : pages) {
            sb.append("# ").append(p.getTitle()).append("\n");
            if (p.getSummary() != null) {
                sb.append(p.getSummary()).append("\n");
            }
            if (p.getContent() != null) {
                sb.append(p.getContent()).append("\n\n");
            }
        }
        return Optional.of(sb.toString());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String stripJson(String s) {
        if (s == null) {
            return "{}";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int idx = t.indexOf('\n');
            if (idx > 0) {
                t = t.substring(idx + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    /** 综合 Gap 报告。 */
    @Data
    public static class GapReport {
        private List<String> isolatedNodes = new ArrayList<>();
        private List<String> bridgeNodes = new ArrayList<>();
        private List<SparseCommunity> sparseCommunities = new ArrayList<>();
        private List<Unanswered> unanswered = new ArrayList<>();
        private List<String> missingTopics = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private String llmError;
    }

    @Data
    public static class SparseCommunity {
        public Integer communityId;
        public List<String> members;
    }

    @Data
    public static class Unanswered {
        public String question;
        public String reason;
        public List<String> suggestedSources = new ArrayList<>();
    }

    private static class LlmGap {
        List<Unanswered> unanswered = new ArrayList<>();
        List<String> missingTopics = new ArrayList<>();
    }
}

package com.example.llmwiki.insight;

import com.example.llmwiki.domain.WikiPage;
import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.ingest.PromptTemplates;
import com.example.llmwiki.llm.ChatClient;
import com.example.llmwiki.repository.WikiPageRepository;
import com.example.llmwiki.retrieval.HybridSearcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主动空白反推：
 * <ol>
 *   <li>基于现有 wiki 概览，让 LLM 站在"用户视角"主动生成多角度候选问题；</li>
 *   <li>每个问题走混合检索 + LLM 判定 answerable/partial/no；</li>
 *   <li>聚合 partial/no 的问题与缺失点，给出"需要补充哪些知识"提示。</li>
 * </ol>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProactiveQuestionAnalyzer {

    private static final String SYSTEM = "You are a strict knowledge base auditor. Always respond in 中文 and output STRICT JSON only.";

    /** 检索得分阈值：低于此值视为"几乎无证据"，跳过 LLM 直接判 no。 */
    private static final double LOW_SCORE_THRESHOLD = 0.005;

    private final WikiPageRepository wikiRepo;
    private final GraphService graphService;
    private final HybridSearcher searcher;
    private final ChatClient chatClient;
    private final PromptTemplates prompts;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 入口。
     *
     * @param maxQuestions 最多生成的候选问题数（10~30 之间合理）
     * @param topK         每个问题检索 top-K
     * @return 主动空白报告
     */
    public ProactiveReport analyze(int maxQuestions, int topK) {
        ProactiveReport report = new ProactiveReport();
        int qCount = Math.max(5, Math.min(30, maxQuestions));
        int k = Math.max(3, Math.min(10, topK));

        List<WikiPage> pages = wikiRepo.findAll();
        if (pages.isEmpty()) {
            report.getSuggestions().add("当前知识库为空，请先导入数据源。");
            return report;
        }

        // 1) 生成候选问题
        List<Candidate> candidates;
        try {
            candidates = generateCandidates(pages, qCount);
        } catch (Exception e) {
            log.warn("生成候选问题失败: {}", e.getMessage());
            report.setLlmError("生成候选问题失败: " + e.getMessage());
            return report;
        }
        log.info("主动空白反推：生成候选问题 {} 个", candidates.size());

        // 2) 逐题评估
        for (Candidate c : candidates) {
            QuestionVerdict v = evaluate(c, k);
            report.getResults().add(v);
        }

        // 3) 聚合建议
        report.setSuggestions(buildSuggestions(report.getResults()));
        report.setSummary(buildSummary(report.getResults()));
        return report;
    }

    /** ============ 候选问题生成 ============ */
    private List<Candidate> generateCandidates(List<WikiPage> pages, int maxCount) throws Exception {
        StringBuilder overview = new StringBuilder();
        Set<String> tagSet = new LinkedHashSet<>();
        Set<String> typeSet = new LinkedHashSet<>();
        int limit = Math.min(pages.size(), 60);
        for (int i = 0; i < limit; i++) {
            WikiPage p = pages.get(i);
            overview.append("- [").append(safe(p.getType())).append("] ")
                    .append(safe(p.getTitle()));
            String sum = p.getSummary();
            if (sum != null && !sum.isBlank()) {
                overview.append("：").append(truncate(sum, 120));
            }
            overview.append('\n');
            if (p.getTags() != null) {
                for (String t : p.getTags().split(",")) {
                    if (!t.isBlank()) {
                        tagSet.add(t.trim());
                    }
                }
            }
            if (p.getType() != null) {
                typeSet.add(p.getType());
            }
        }
        StringBuilder topics = new StringBuilder();
        topics.append("types: ").append(String.join(", ", typeSet)).append('\n');
        topics.append("tags: ").append(String.join(", ", tagSet)).append('\n');
        // 拼接桥节点提示，便于 LLM 看到跨域枢纽
        List<String> bridges = graphService.bridgeNodes();
        if (bridges != null && !bridges.isEmpty()) {
            topics.append("bridge_nodes: ").append(String.join(", ", bridges)).append('\n');
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("overview", truncate(overview.toString(), 4000));
        vars.put("topics", truncate(topics.toString(), 1500));
        vars.put("maxCount", String.valueOf(maxCount));

        String userPrompt = prompts.render("proactive_questions", vars);
        String resp = chatClient.complete(SYSTEM, userPrompt);

        List<Candidate> out = new ArrayList<>();
        Set<String> dedup = new HashSet<>();
        JsonNode root = objectMapper.readTree(stripJson(resp));
        if (root.has("questions")) {
            for (JsonNode n : root.get("questions")) {
                Candidate c = new Candidate();
                c.question = n.path("question").asText("").trim();
                c.angle = n.path("angle").asText("what");
                c.topic = n.path("topic").asText("");
                if (c.question.isEmpty() || !dedup.add(c.question)) {
                    continue;
                }
                out.add(c);
                if (out.size() >= maxCount) {
                    break;
                }
            }
        }
        return out;
    }

    /** ============ 单题评估 ============ */
    private QuestionVerdict evaluate(Candidate c, int topK) {
        QuestionVerdict v = new QuestionVerdict();
        v.question = c.question;
        v.angle = c.angle;
        v.topic = c.topic;

        List<HybridSearcher.SearchHit> hits;
        try {
            hits = searcher.search(c.question, topK);
        } catch (Exception e) {
            log.warn("评估问题检索失败 q={} err={}", c.question, e.getMessage());
            v.verdict = "no";
            v.reason = "检索失败：" + e.getMessage();
            return v;
        }
        v.evidence = new ArrayList<>();
        double topScore = 0;
        for (HybridSearcher.SearchHit h : hits) {
            EvidenceRef e = new EvidenceRef();
            e.slug = h.getSlug();
            e.title = h.getTitle();
            e.score = h.getScore();
            v.evidence.add(e);
            if (h.getScore() != null) {
                topScore = Math.max(topScore, h.getScore());
            }
        }

        // 证据极弱：跳过 LLM 直判
        if (hits.isEmpty() || topScore < LOW_SCORE_THRESHOLD) {
            v.verdict = "no";
            v.reason = "知识库中未检索到与问题相关的内容。";
            v.missingPoints = List.of("缺乏该主题的基础资料");
            v.suggestedSources = List.of("围绕该主题导入官方文档或专题文章");
            return v;
        }

        // LLM 判定
        try {
            StringBuilder hitsBlock = new StringBuilder();
            int idx = 0;
            for (HybridSearcher.SearchHit h : hits) {
                idx++;
                hitsBlock.append("[").append(idx).append("] ").append(safe(h.getTitle()));
                if (h.getSummary() != null && !h.getSummary().isBlank()) {
                    hitsBlock.append(" :: ").append(truncate(h.getSummary(), 240));
                }
                hitsBlock.append('\n');
            }
            Map<String, String> vars = new HashMap<>();
            vars.put("question", c.question);
            vars.put("hits", hitsBlock.toString());
            String resp = chatClient.complete(SYSTEM, prompts.render("answerability", vars));
            JsonNode root = objectMapper.readTree(stripJson(resp));
            v.verdict = root.path("verdict").asText("partial");
            v.missingPoints = readArray(root.get("missing_points"));
            v.suggestedSources = readArray(root.get("suggested_sources"));
        } catch (Exception e) {
            log.warn("LLM 判定失败 q={} err={}", c.question, e.getMessage());
            v.verdict = "partial";
            v.reason = "LLM 判定失败：" + e.getMessage();
        }
        return v;
    }

    /** ============ 聚合 ============ */
    private List<String> buildSuggestions(List<QuestionVerdict> results) {
        Map<String, Integer> sourceCount = new HashMap<>();
        Map<String, Integer> pointCount = new HashMap<>();
        for (QuestionVerdict v : results) {
            if (!"answerable".equalsIgnoreCase(v.verdict)) {
                if (v.suggestedSources != null) {
                    for (String s : v.suggestedSources) {
                        sourceCount.merge(s, 1, Integer::sum);
                    }
                }
                if (v.missingPoints != null) {
                    for (String s : v.missingPoints) {
                        pointCount.merge(s, 1, Integer::sum);
                    }
                }
            }
        }
        List<String> out = new ArrayList<>();
        sourceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> out.add("[补充资料] " + e.getKey() + "（被 " + e.getValue() + " 个问题需要）"));
        pointCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> out.add("[缺失要点] " + e.getKey() + "（出现 " + e.getValue() + " 次）"));
        if (out.isEmpty()) {
            out.add("候选问题大多可被回答，当前知识库覆盖良好。");
        }
        return out;
    }

    private Map<String, Integer> buildSummary(List<QuestionVerdict> results) {
        Map<String, Integer> m = new HashMap<>();
        m.put("total", results.size());
        m.put("answerable", 0);
        m.put("partial", 0);
        m.put("no", 0);
        for (QuestionVerdict v : results) {
            String key = v.verdict == null ? "no" : v.verdict.toLowerCase();
            m.merge(key, 1, Integer::sum);
        }
        // 排序后填回（保留全部 key）
        Map<String, Integer> ordered = new HashMap<>(m);
        return ordered;
    }

    /** ============ utils ============ */
    private static List<String> readArray(JsonNode arr) {
        if (arr == null || !arr.isArray()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String s = n.asText();
            if (s != null && !s.isBlank()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
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

    /** 候选问题（生成阶段）。 */
    @Data
    public static class Candidate {
        public String question;
        public String angle;
        public String topic;
    }

    /** 单题判定结果。 */
    @Data
    public static class QuestionVerdict {
        public String question;
        public String angle;
        public String topic;
        /** answerable | partial | no */
        public String verdict;
        public String reason;
        public List<String> missingPoints = new ArrayList<>();
        public List<String> suggestedSources = new ArrayList<>();
        public List<EvidenceRef> evidence = new ArrayList<>();
    }

    /** 检索命中证据。 */
    @Data
    public static class EvidenceRef {
        public String slug;
        public String title;
        public Double score;
    }

    /** 综合报告。 */
    @Data
    public static class ProactiveReport {
        private List<QuestionVerdict> results = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private Map<String, Integer> summary = new HashMap<>();
        private String llmError;
    }
}

package com.example.llmwiki.ingest;

import com.example.llmwiki.domain.AnalysisResult;
import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.domain.SourceRecord;
import com.example.llmwiki.domain.WikiPage;
import com.example.llmwiki.domain.WikiPageDraft;
import com.example.llmwiki.graph.EntityAliasService;
import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.graph.LouvainCommunityDetector;
import com.example.llmwiki.llm.ChatClient;
import com.example.llmwiki.llm.EmbeddingClient;
import com.example.llmwiki.llm.LlmException;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.ParserRegistry;
import com.example.llmwiki.progress.ProgressBus;
import com.example.llmwiki.progress.ProgressEvent;
import com.example.llmwiki.repository.SourceRecordRepository;
import com.example.llmwiki.repository.WikiPageRepository;
import com.example.llmwiki.retrieval.LuceneIndexer;
import com.example.llmwiki.util.TextUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摄入流水线（两步式 CoT）：
 * <ol>
 *   <li>PARSE：调用解析器获取 RawDocument；</li>
 *   <li>ANALYZE：LLM 输出结构化分析 JSON；</li>
 *   <li>GENERATE：LLM 输出多页 wiki JSON；</li>
 *   <li>INDEX/GRAPH：落库 + 文件 + Lucene + 图谱。</li>
 * </ol>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestPipeline {

    private static final int CONTENT_LIMIT = 12_000;

    private final ParserRegistry parserRegistry;
    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final PromptTemplates prompts;
    private final WikiPageRepository wikiRepo;
    private final SourceRecordRepository sourceRepo;
    private final WikiFileWriter wikiWriter;
    private final LuceneIndexer luceneIndexer;
    private final GraphService graphService;
    private final LouvainCommunityDetector louvain;
    private final EntityAliasService aliasService;
    private final ProgressBus progressBus;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void run(Long taskId, SourceRecord source, byte[] fileBytes, String mime) throws Exception {
        publish(taskId, source.getDisplayName(), "PARSE", 5, "RUNNING", "开始解析");
        ParseRequest req = ParseRequest.builder()
                .kind(source.getKind())
                .ref(source.getRef())
                .displayName(source.getDisplayName())
                .fileBytes(fileBytes)
                .mime(mime)
                .build();
        RawDocument raw = parserRegistry.parse(req);

        // 增量缓存
        if (source.getContentHash() != null && source.getContentHash().equals(raw.getContentHash())) {
            publish(taskId, source.getDisplayName(), "SKIP", 100, "SKIPPED", "内容未变，跳过");
            return;
        }

        publish(taskId, source.getDisplayName(), "ANALYZE", 25, "RUNNING", "Step1 LLM 分析中");
        AnalysisResult analysis = stepAnalyze(raw);

        publish(taskId, source.getDisplayName(), "GENERATE", 55, "RUNNING", "Step2 LLM 生成页面中");
        List<WikiPageDraft> drafts = stepGenerate(raw, analysis);

        publish(taskId, source.getDisplayName(), "INDEX", 80, "RUNNING", "落库与索引中");
        List<WikiPage> persisted = persistPages(drafts, raw);

        publish(taskId, source.getDisplayName(), "GRAPH", 92, "RUNNING", "更新图谱与社区");
        for (WikiPage p : persisted) {
            List<String> outLinks = csvToList(p.getOutLinks());
            List<String> sources = csvToList(p.getSources());
            List<String> tags = csvToList(p.getTags());
            graphService.upsertPage(p, outLinks, sources, tags);
        }
        louvain.detectAndAssign(graphService);
        graphService.persist();
        aliasService.persist();

        // 更新 source hash
        source.setContentHash(raw.getContentHash());
        source.setLastFetchedAt(Instant.now());
        sourceRepo.save(source);

        wikiWriter.appendLog("ingest [" + source.getKind() + "] " + source.getDisplayName()
                + " -> " + persisted.size() + " pages");

        publish(taskId, source.getDisplayName(), "DONE", 100, "SUCCESS", "完成，共生成 " + persisted.size() + " 页");
    }

    private AnalysisResult stepAnalyze(RawDocument raw) {
        Map<String, String> vars = new HashMap<>();
        vars.put("overview", buildOverview());
        vars.put("sourceKind", raw.getSourceKind());
        vars.put("displayName", raw.getDisplayName());
        vars.put("content", TextUtils.truncate(raw.getText(), CONTENT_LIMIT)
                + "\n\n" + String.join("\n", raw.getImageCaptions()));
        String prompt = prompts.render("analyze", vars);
        String json = chatClient.complete("你是知识库结构编辑，严格输出 JSON。", prompt);
        try {
            JsonNode n = objectMapper.readTree(stripJson(json));
            AnalysisResult r = AnalysisResult.builder()
                    .summary(n.path("summary").asText(""))
                    .build();
            n.path("entities").forEach(e -> r.getEntities().add(e.asText()));
            n.path("concepts").forEach(e -> r.getConcepts().add(e.asText()));
            n.path("connections").forEach(e -> r.getConnections().add(e.asText()));
            n.path("contradictions").forEach(e -> r.getContradictions().add(e.asText()));
            n.path("recommended").forEach(e -> r.getRecommended().add(
                    AnalysisResult.PageOutline.builder()
                            .type(e.path("type").asText("entity"))
                            .title(e.path("title").asText(""))
                            .slug(e.path("slug").asText(""))
                            .build()));
            return r;
        } catch (Exception e) {
            throw new IngestException("Step1 解析 LLM JSON 失败: " + e.getMessage(), e);
        }
    }

    private List<WikiPageDraft> stepGenerate(RawDocument raw, AnalysisResult analysis) {
        Map<String, String> vars = new HashMap<>();
        try {
            vars.put("analysis", objectMapper.writeValueAsString(analysis));
        } catch (Exception ignore) {
            vars.put("analysis", "{}");
        }
        vars.put("content", TextUtils.truncate(raw.getText(), CONTENT_LIMIT));
        vars.put("sourceKind", raw.getSourceKind());
        vars.put("displayName", raw.getDisplayName());
        vars.put("sourceSlug", "source-" + TextUtils.slugify(raw.getDisplayName()));
        String prompt = prompts.render("generate", vars);
        String json = chatClient.complete("你是知识库内容生成器，严格输出 JSON。", prompt);
        List<WikiPageDraft> drafts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(stripJson(json));
            for (JsonNode p : root.path("pages")) {
                WikiPageDraft d = WikiPageDraft.builder()
                        .type(p.path("type").asText("entity"))
                        .title(p.path("title").asText(""))
                        .slug(TextUtils.slugify(p.path("slug").asText(p.path("title").asText("page"))))
                        .summary(p.path("summary").asText(""))
                        .body(p.path("body").asText(""))
                        .build();
                p.path("tags").forEach(t -> d.getTags().add(t.asText()));
                p.path("out_links").forEach(t -> d.getOutLinks().add(t.asText()));
                d.getSources().add(raw.getSourceRef());
                drafts.add(d);
            }
        } catch (Exception e) {
            throw new IngestException("Step2 解析 LLM JSON 失败: " + e.getMessage(), e);
        }
        if (drafts.isEmpty()) {
            throw new IngestException("Step2 未生成任何页面");
        }
        return drafts;
    }

    private List<WikiPage> persistPages(List<WikiPageDraft> drafts, RawDocument raw) throws Exception {
        List<WikiPage> result = new ArrayList<>();
        // 先做全量归一化：draft.slug -> canonical slug，后续 out_links 可以复用
        java.util.Map<String, String> slugMap = new java.util.HashMap<>();
        for (WikiPageDraft d : drafts) {
            String canonical = aliasService.canonicalize(d.getType(), d.getTitle(), d.getSlug());
            slugMap.put(d.getSlug(), canonical);
            d.setSlug(canonical);
        }
        for (WikiPageDraft d : drafts) {
            // out_links 同样归一化：优先查本次生成的 slugMap，再打 alias 表
            java.util.List<String> normalizedLinks = new java.util.ArrayList<>();
            for (String l : d.getOutLinks()) {
                String n = slugMap.getOrDefault(l, l);
                normalizedLinks.add(n);
            }
            normalizedLinks = aliasService.canonicalizeLinks(normalizedLinks);
            d.setOutLinks(normalizedLinks);

            WikiPage page = wikiRepo.findBySlug(d.getSlug()).orElseGet(() ->
                    WikiPage.builder().slug(d.getSlug()).createdAt(Instant.now()).build());
            page.setTitle(d.getTitle());
            page.setType(d.getType());
            page.setSummary(mergeText(page.getSummary(), d.getSummary()));
            page.setContent(mergeText(page.getContent(), d.getBody()));
            page.setOutLinks(mergeCsv(page.getOutLinks(), String.join(",", d.getOutLinks())));
            page.setSources(mergeCsv(page.getSources(), String.join(",", d.getSources())));
            page.setTags(mergeCsv(page.getTags(), String.join(",", d.getTags())));
            page.setUpdatedAt(Instant.now());
            page = wikiRepo.save(page);
            wikiWriter.writePage(page);

            // Embedding 同步进 Lucene
            float[] vec;
            try {
                String embeddable = (page.getTitle() + "\n" + page.getSummary() + "\n"
                        + TextUtils.truncate(page.getContent(), 4_000));
                vec = embeddingClient.embed(embeddable);
            } catch (LlmException e) {
                log.warn("Embedding 失败，仅做 BM25 索引: {}", e.getMessage());
                vec = new float[0];
            }
            luceneIndexer.upsert(page, vec);
            result.add(page);
        }
        return result;
    }

    private String buildOverview() {
        long n = wikiRepo.count();
        return "当前 wiki 共 " + n + " 页，节点 " + graphService.nodes().size()
                + "、边 " + graphService.totalEdges() + "。";
    }

    private List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 合并两个 CSV，去重保顺。 */
    private String mergeCsv(String oldCsv, String newCsv) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (String s : csvToList(oldCsv)) { set.add(s); }
        for (String s : csvToList(newCsv)) { set.add(s); }
        return String.join(",", set);
    }

    /** 合并正文/摘要：新老不同时以 \n\n--- \n\n 拼接，避免覆盖丢信息。 */
    private String mergeText(String oldText, String newText) {
        if (oldText == null || oldText.isBlank()) { return newText; }
        if (newText == null || newText.isBlank()) { return oldText; }
        if (oldText.equals(newText) || oldText.contains(newText)) { return oldText; }
        return oldText + "\n\n---\n\n" + newText;
    }

    /**
     * 兼容 LLM 偶尔输出 ```json ... ``` 包裹的情况，并对被截断的 JSON 做左右括号补齐兑底。
     */
    private String stripJson(String s) {
        if (s == null) {
            return "{}";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        t = t.trim();
        // 对被截断的 JSON 做括号补齐（忍受字符串内的括号不能完美处理，但能覆盖绝大多数场景）
        t = balanceBrackets(t);
        return t;
    }

    /**
     * 为被截断的 JSON 补齐剩余的 ] 与 } 。字符串内的括号会被跳过。
     */
    private String balanceBrackets(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int curly = 0;
        int square = 0;
        boolean inStr = false;
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inStr = !inStr;
                continue;
            }
            if (inStr) {
                continue;
            }
            switch (c) {
                case '{': curly++; break;
                case '}': curly--; break;
                case '[': square++; break;
                case ']': square--; break;
                default: break;
            }
        }
        StringBuilder sb = new StringBuilder(s);
        // 被截断在字符串内，补上右引号
        if (inStr) {
            sb.append('"');
        }
        // 补齐剩余的 ] 和 }
        while (square-- > 0) {
            sb.append(']');
        }
        while (curly-- > 0) {
            sb.append('}');
        }
        return sb.toString();
    }

    private void publish(Long taskId, String name, String stage, int percent, String status, String msg) {
        progressBus.publish(ProgressEvent.builder()
                .taskId(taskId).displayName(name).stage(stage).percent(percent)
                .status(status).message(msg).timestamp(Instant.now()).build());
    }
}

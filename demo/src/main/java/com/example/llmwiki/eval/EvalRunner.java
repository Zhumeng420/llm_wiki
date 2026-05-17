package com.example.llmwiki.eval;

import com.example.llmwiki.domain.EvalReport;
import com.example.llmwiki.llm.ChatClient;
import com.example.llmwiki.llm.LlmException;
import com.example.llmwiki.repository.EvalReportRepository;
import com.example.llmwiki.retrieval.HybridSearcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 评测运行器：读取 question/expected_slugs CSV，对每条 query 执行混合检索，
 * 计算 answerRate / hitRate@5 / avgRelevance / avgLatency 等指标。
 * <p>
 * CSV 格式（UTF-8，首行为 header）：
 * <pre>
 *   question,expected_slugs
 *   "什么是 X","x;x-overview"
 * </pre>
 * expected_slugs 用分号分隔，多个候选答案中只要命中任意一个即视为命中。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalRunner {

    private static final int TOP_K = 5;
    private static final String JUDGE_SYS = "You are a strict relevance judge. Only output an integer 0-5, no extra words.";

    private final HybridSearcher searcher;
    private final ChatClient chatClient;
    private final EvalReportRepository reportRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 主入口：执行评测并落库。
     *
     * @param name      报告名
     * @param csvBytes  CSV 文件字节流
     * @param useJudge  是否调用 LLM 评分相关性
     */
    public EvalReport run(String name, byte[] csvBytes, boolean useJudge) {
        List<EvalCase> cases = parseCsv(csvBytes);
        log.info("评测开始: name={} 总数={}", name, cases.size());

        List<CaseResult> results = new ArrayList<>();
        int answered = 0;
        int hitAt5 = 0;
        double sumRelevance = 0;
        long sumLatency = 0;

        for (EvalCase c : cases) {
            CaseResult r = new CaseResult();
            r.question = c.question;
            r.expected = c.expected;
            long start = System.currentTimeMillis();
            try {
                List<HybridSearcher.SearchHit> hits = searcher.search(c.question, TOP_K);
                r.latencyMs = System.currentTimeMillis() - start;
                r.hits = hits;
                if (!hits.isEmpty()) {
                    answered++;
                    r.answered = true;
                }
                Set<String> hitSlugs = new HashSet<>();
                for (HybridSearcher.SearchHit h : hits) {
                    hitSlugs.add(h.getSlug());
                }
                if (c.expected != null && !c.expected.isEmpty()) {
                    for (String exp : c.expected) {
                        if (hitSlugs.contains(exp)) {
                            r.hit = true;
                            break;
                        }
                    }
                    if (r.hit) {
                        hitAt5++;
                    }
                }
                if (useJudge && !hits.isEmpty()) {
                    r.relevance = judgeRelevance(c.question, hits.get(0));
                    sumRelevance += r.relevance;
                }
            } catch (Exception e) {
                r.error = e.getMessage();
                log.warn("评测条目失败 q={} err={}", c.question, e.getMessage());
            }
            sumLatency += r.latencyMs;
            results.add(r);
        }

        int total = cases.size();
        double answerRate = total == 0 ? 0 : (double) answered / total;
        double hitRate = total == 0 ? 0 : (double) hitAt5 / total;
        double avgRelevance = answered == 0 ? 0 : sumRelevance / answered;
        long avgLatency = total == 0 ? 0 : sumLatency / total;

        EvalReport report = EvalReport.builder()
                .name(name)
                .total(total)
                .answered(answered)
                .answerRate(answerRate)
                .hitRateAt5(hitRate)
                .avgRelevance(avgRelevance)
                .avgLatencyMs(avgLatency)
                .createdAt(Instant.now())
                .build();
        try {
            report.setDetails(objectMapper.writeValueAsString(results));
        } catch (Exception e) {
            log.warn("序列化评测明细失败: {}", e.getMessage());
        }
        return reportRepo.save(report);
    }

    /**
     * 调用 LLM 给单条召回结果打分（0-5）。
     */
    private double judgeRelevance(String question, HybridSearcher.SearchHit hit) {
        try {
            String user = "问题：" + question + "\n候选回答标题：" + hit.getTitle()
                    + "\n候选回答摘要：" + (hit.getSummary() == null ? "" : hit.getSummary())
                    + "\n请给出与问题的相关性评分（0-5），只输出数字。";
            String resp = chatClient.complete(JUDGE_SYS, user).trim();
            // 只取首个数字字符
            StringBuilder sb = new StringBuilder();
            for (char ch : resp.toCharArray()) {
                if (Character.isDigit(ch) || ch == '.') {
                    sb.append(ch);
                } else if (sb.length() > 0) {
                    break;
                }
            }
            return sb.length() == 0 ? 0 : Math.min(5, Math.max(0, Double.parseDouble(sb.toString())));
        } catch (LlmException e) {
            log.debug("Judge 不可用: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            log.warn("Judge 失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 解析 CSV：header + (question,expected_slugs) 行。
     * 简单实现，不支持引号内换行。
     */
    private List<EvalCase> parseCsv(byte[] csvBytes) {
        List<EvalCase> out = new ArrayList<>();
        try (InputStream in = new java.io.ByteArrayInputStream(csvBytes);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                List<String> cols = splitCsv(line);
                if (cols.isEmpty()) {
                    continue;
                }
                EvalCase c = new EvalCase();
                c.question = cols.get(0).trim();
                if (cols.size() > 1 && !cols.get(1).isBlank()) {
                    c.expected = Arrays.stream(cols.get(1).split("[;,，]"))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                } else {
                    c.expected = List.of();
                }
                out.add(c);
            }
        } catch (Exception e) {
            log.warn("解析 CSV 失败: {}", e.getMessage());
        }
        return out;
    }

    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    @Data
    public static class EvalCase {
        private String question;
        private List<String> expected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaseResult {
        private String question;
        private List<String> expected;
        private List<HybridSearcher.SearchHit> hits;
        private boolean answered;
        private boolean hit;
        private double relevance;
        private long latencyMs;
        private String error;
    }
}

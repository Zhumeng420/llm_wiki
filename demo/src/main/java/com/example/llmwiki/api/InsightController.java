package com.example.llmwiki.api;

import com.example.llmwiki.insight.GapAnalyzer;
import com.example.llmwiki.insight.ProactiveQuestionAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识空白反推接口。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final GapAnalyzer gapAnalyzer;
    private final ProactiveQuestionAnalyzer proactiveAnalyzer;

    /**
     * @param useLlm 是否调用 LLM 进行语义审计；默认 true。无 key 时建议传 false。
     */
    @GetMapping("/gap")
    public GapAnalyzer.GapReport gap(@RequestParam(value = "useLlm", defaultValue = "true") boolean useLlm) {
        return gapAnalyzer.analyze(useLlm);
    }

    /**
     * 主动空白反推：系统自己猜测用户可能提的问题 → 逐题检索 + LLM 判定能否回答 → 给出补充建议。
     *
     * @param count 候选问题数量上限，默认 15
     * @param topK  每题检索 top-K，默认 5
     */
    @GetMapping("/proactive-gap")
    public ProactiveQuestionAnalyzer.ProactiveReport proactive(
            @RequestParam(value = "count", defaultValue = "15") int count,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        return proactiveAnalyzer.analyze(count, topK);
    }
}

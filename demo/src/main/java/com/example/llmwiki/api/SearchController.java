package com.example.llmwiki.api;

import com.example.llmwiki.retrieval.HybridSearcher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 检索接口：BM25 + 向量混合。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final HybridSearcher searcher;

    @GetMapping
    public List<HybridSearcher.SearchHit> search(@RequestParam("q") String q,
                                                 @RequestParam(value = "topK", defaultValue = "10") int topK)
            throws Exception {
        return searcher.search(q, topK);
    }
}

package com.example.llmwiki.retrieval;

import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.llm.EmbeddingClient;
import com.example.llmwiki.llm.LlmException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索器：BM25 + 向量 KNN，使用 RRF 融合。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearcher {

    private static final int RRF_K = 60;

    private final LuceneIndexer indexer;
    private final EmbeddingClient embeddingClient;
    private final GraphService graphService;

    public List<SearchHit> search(String queryText, int topK) throws Exception {
        Map<String, Double> rrf = new HashMap<>();
        Map<String, SearchHit> hits = new HashMap<>();
        int n = Math.max(topK, 10);

        IndexSearcher searcher = indexer.openSearcher();

        // BM25
        try {
            QueryParser parser = new QueryParser("content", indexer.analyzer());
            parser.setDefaultOperator(QueryParser.Operator.OR);
            Query q = parser.parse(QueryParser.escape(queryText));
            TopDocs td = searcher.search(q, n);
            int rank = 0;
            for (ScoreDoc sd : td.scoreDocs) {
                rank++;
                Document doc = searcher.storedFields().document(sd.doc);
                String slug = doc.get("slug");
                rrf.merge(slug, 1.0 / (RRF_K + rank), Double::sum);
                hits.computeIfAbsent(slug, k -> toHit(doc, sd.score, "bm25"));
            }
        } catch (Exception e) {
            log.warn("BM25 检索失败: {}", e.getMessage());
        }

        // KNN
        try {
            float[] vec = embeddingClient.embed(queryText);
            if (vec.length > 0) {
                Query knn = new KnnFloatVectorQuery("vector", vec, n);
                TopDocs td = searcher.search(knn, n);
                int rank = 0;
                for (ScoreDoc sd : td.scoreDocs) {
                    rank++;
                    Document doc = searcher.storedFields().document(sd.doc);
                    String slug = doc.get("slug");
                    rrf.merge(slug, 1.0 / (RRF_K + rank), Double::sum);
                    hits.computeIfAbsent(slug, k -> toHit(doc, sd.score, "knn"));
                }
            }
        } catch (LlmException e) {
            log.warn("Embedding 不可用，降级为 BM25 单通: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("KNN 检索失败: {}", e.getMessage());
        }

        // 图谱 boost：与命中节点关联度高的邻居加分
        Map<String, Double> boost = new HashMap<>();
        for (String slug : hits.keySet()) {
            for (Map.Entry<String, Double> e : graphService.adjacency().getOrDefault(slug, Map.of()).entrySet()) {
                boost.merge(e.getKey(), e.getValue() * 0.001, Double::sum);
            }
        }
        for (Map.Entry<String, Double> e : boost.entrySet()) {
            rrf.merge(e.getKey(), e.getValue(), Double::sum);
        }

        List<SearchHit> result = new ArrayList<>();
        rrf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(e -> {
                    SearchHit h = hits.get(e.getKey());
                    if (h != null) {
                        h.setScore(e.getValue());
                        result.add(h);
                    }
                });
        return result;
    }

    private SearchHit toHit(Document doc, double score, String src) {
        return SearchHit.builder()
                .slug(doc.get("slug"))
                .title(doc.get("title"))
                .summary(doc.get("summary"))
                .type(doc.get("type"))
                .source(src)
                .score(score)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class SearchHit {
        private String slug;
        private String title;
        private String type;
        private String summary;
        private String source;
        private Double score;
    }
}

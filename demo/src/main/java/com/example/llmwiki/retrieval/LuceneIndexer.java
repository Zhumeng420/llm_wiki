package com.example.llmwiki.retrieval;

import com.example.llmwiki.config.LlmProperties;
import com.example.llmwiki.config.StorageProperties;
import com.example.llmwiki.domain.WikiPage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

/**
 * Lucene 索引器：单一索引同时支撑 BM25 全文检索与 KNN 向量检索。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LuceneIndexer {

    private final StorageProperties storageProperties;
    private final LlmProperties llmProperties;

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter writer;

    @PostConstruct
    public void init() throws Exception {
        File dir = new File(storageProperties.getIndexDir());
        dir.mkdirs();
        directory = FSDirectory.open(Path.of(dir.getAbsolutePath()));
        analyzer = new SmartChineseAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(directory, cfg);
        writer.commit();
        log.info("Lucene 索引初始化: {}", dir.getAbsolutePath());
    }

    @PreDestroy
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (directory != null) {
                directory.close();
            }
        } catch (Exception e) {
            log.warn("关闭 Lucene 失败: {}", e.getMessage());
        }
    }

    /**
     * upsert 单页（带可选向量）。
     */
    public synchronized void upsert(WikiPage page, float[] vector) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("slug", page.getSlug(), Field.Store.YES));
        doc.add(new StringField("type", page.getType() == null ? "" : page.getType(), Field.Store.YES));
        doc.add(new TextField("title", nullSafe(page.getTitle()), Field.Store.YES));
        doc.add(new TextField("summary", nullSafe(page.getSummary()), Field.Store.YES));
        doc.add(new TextField("content", nullSafe(page.getContent()), Field.Store.YES));
        doc.add(new TextField("tags", nullSafe(page.getTags()), Field.Store.YES));
        if (vector != null && vector.length > 0) {
            int dim = llmProperties.getEmbedding().getDimensions();
            float[] use = vector;
            if (use.length != dim) {
                // 兜底：长度对齐
                float[] resized = new float[dim];
                System.arraycopy(use, 0, resized, 0, Math.min(use.length, dim));
                use = resized;
            }
            doc.add(new KnnFloatVectorField("vector", use, VectorSimilarityFunction.COSINE));
        }
        writer.updateDocument(new Term("slug", page.getSlug()), doc);
        writer.commit();
    }

    public synchronized void delete(String slug) throws Exception {
        writer.deleteDocuments(new Term("slug", slug));
        writer.commit();
    }

    public IndexSearcher openSearcher() throws Exception {
        return new IndexSearcher(DirectoryReader.open(writer));
    }

    public Analyzer analyzer() {
        return analyzer;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}

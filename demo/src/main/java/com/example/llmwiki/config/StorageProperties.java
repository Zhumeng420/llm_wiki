package com.example.llmwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储相关路径配置。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm-wiki.storage")
public class StorageProperties {

    /** 数据根目录 */
    private String rootDir = "./data";
    /** 原始资料目录（不可变） */
    private String rawDir = "./data/raw";
    /** Wiki Markdown 输出目录 */
    private String wikiDir = "./data/wiki";
    /** Lucene 索引目录 */
    private String indexDir = "./data/index";
    /** 图谱持久化目录 */
    private String graphDir = "./data/graph";
}

package com.example.llmwiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LLM-Wiki 应用启动类。
 * <p>
 * 基于 Andrej Karpathy 的 llm-wiki 模式，将 LLM 作为知识库的"长期记忆维护者"，
 * 把异构源（PDF/Word/Excel/图片/网页/飞书/钉钉等）增量编译为带交叉引用的 Wiki，
 * 并自动构建知识图谱、识别知识空白、定时刷新与可量化评估。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LlmWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmWikiApplication.class, args);
    }

}

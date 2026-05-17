package com.example.llmwiki.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 摄入任务（持久化串行队列项）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ingest_task")
public class IngestTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联 source_record.id */
    private Long sourceId;

    /** PENDING / RUNNING / SUCCESS / FAILED / CANCELLED / SKIPPED */
    @Column(length = 16, nullable = false)
    private String status;

    /** 当前阶段：PARSE / ANALYZE / GENERATE / INDEX / GRAPH */
    @Column(length = 32)
    private String stage;

    /** 进度百分比 0-100 */
    private Integer percent;

    /** 重试次数 */
    private Integer retryCount;

    private Instant createdAt;

    private Instant startedAt;

    private Instant finishedAt;

    @Lob
    @Column(name = "error_msg")
    private String errorMessage;
}

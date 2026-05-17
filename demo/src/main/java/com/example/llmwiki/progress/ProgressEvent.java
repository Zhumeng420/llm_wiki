package com.example.llmwiki.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 进度事件（任务粒度）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {

    /** 任务 id */
    private Long taskId;

    /** 来源展示名 */
    private String displayName;

    /** 阶段：QUEUED / PARSE / ANALYZE / GENERATE / INDEX / GRAPH / DONE / FAIL / SKIP */
    private String stage;

    /** 0-100 */
    private Integer percent;

    /** 描述 */
    private String message;

    /** 状态：PENDING / RUNNING / SUCCESS / FAILED / SKIPPED */
    private String status;

    @Builder.Default
    private Instant timestamp = Instant.now();
}

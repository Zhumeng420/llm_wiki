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
 * 评测报告。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eval_report")
public class EvalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 256)
    private String name;

    private Integer total;
    private Integer answered;
    private Double answerRate;
    private Double hitRateAt5;
    private Double avgRelevance;
    private Long avgLatencyMs;

    /** 详细 JSON */
    @Lob
    private String details;

    private Instant createdAt;
}

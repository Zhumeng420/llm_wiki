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
 * 数据源记录（每一次摄入的原始来源）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "source_record")
public class SourceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FILE / URL / FEISHU / DINGTALK */
    @Column(length = 32, nullable = false)
    private String kind;

    /** 文件名 / URL / 文档 token */
    @Column(length = 1024, nullable = false)
    private String ref;

    /** 展示名 */
    @Column(length = 512)
    private String displayName;

    /** 当前内容指纹 */
    @Column(length = 128)
    private String contentHash;

    /** 是否启用定时刷新 */
    private Boolean watchEnabled;

    /** 创建时间 */
    private Instant createdAt;

    /** 上次刷新时间 */
    private Instant lastFetchedAt;

    /** 备注 */
    @Lob
    private String note;
}

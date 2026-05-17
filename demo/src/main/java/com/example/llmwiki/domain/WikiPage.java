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
 * Wiki 页面（生成后落库 + 落 Markdown）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wiki_page")
public class WikiPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 页面 slug，唯一，作为 [[wikilink]] 目标 */
    @Column(length = 256, nullable = false, unique = true)
    private String slug;

    /** 页面标题 */
    @Column(length = 512, nullable = false)
    private String title;

    /** 页面类型：entity / concept / source / overview / index / log / purpose */
    @Column(length = 32, nullable = false)
    private String type;

    /** 摘要 */
    @Column(length = 2048)
    private String summary;

    /** 正文（Markdown） */
    @Lob
    private String content;

    /** 关联来源 ref，逗号分隔 */
    @Column(length = 2048)
    private String sources;

    /** 标签，逗号分隔 */
    @Column(length = 1024)
    private String tags;

    /** 链接出去的 slug，逗号分隔（用于图谱构建） */
    @Lob
    @Column(name = "out_links")
    private String outLinks;

    private Instant createdAt;

    private Instant updatedAt;
}

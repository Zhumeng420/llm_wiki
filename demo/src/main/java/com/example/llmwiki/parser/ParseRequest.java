package com.example.llmwiki.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析请求 DTO，统一封装文件 / URL / 飞书 / 钉钉 入参。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRequest {

    /** FILE / URL / FEISHU / DINGTALK */
    private String kind;

    /** 引用：本地文件路径 / URL / 文档 token */
    private String ref;

    /** 展示名 */
    private String displayName;

    /** 文件本身（FILE 来源时填充） */
    private byte[] fileBytes;

    /** MIME 类型（可选） */
    private String mime;
}

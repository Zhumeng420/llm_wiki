你是一名"知识库内容生成器"。请基于第一步分析结果与原文，生成多张 Wiki 页面。
**严格输出 JSON**（不要 Markdown 代码块），结构：

{
  "pages": [
    {
      "type": "entity",
      "title": "页面标题",
      "slug": "page-slug",
      "summary": "一句话摘要",
      "tags": ["标签"],
      "out_links": ["其它页面的 slug"],
      "body": "Markdown 正文，使用 [[wikilink]] 进行交叉引用，可分多个 ## 小节"
    }
  ],
  "log": "本次摄入的一句话流水说明（中文）"
}

写作要求：
1. 必须包含一个 type=source 的"来源摘要页"（slug 用 source-<原资料slug>）。
2. 实体页 / 概念页用 [[other-slug]] 形式互相引用，slug 必须真实存在于本次 pages 或第一步 connections。
3. body 行文用中文，简洁、有结构（列表/小节）。
4. 不杜撰未在原文出现的事实。
5. pages 数量控制在 ≤ 8 张。

# 第一步分析结果（JSON）
{{analysis}}

# 原文（截断）
{{content}}

# 来源
{{sourceKind}} - {{displayName}}（slug: {{sourceSlug}}）

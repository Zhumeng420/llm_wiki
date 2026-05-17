---
title: "函数计算 FC"
slug: "aliyun-fc"
type: "entity"
summary: "阿里云提供的无服务器（Serverless）计算服务，支持事件驱动、弹性伸缩的代码执行。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [Serverless,云服务,运行时]
updated_at: 2026-05-17T14:51:35.133889600Z
---

# 函数计算 FC

## 基本能力
- 按需执行函数，免运维底层基础设施；
- 自动扩缩容，应对 AI Agent 的突发请求负载；
- 内置 Sandbox 运行环境，保障函数间强隔离。

## 在 AI Agent 中的作用
- 作为 AI Agent 的执行单元载体，承载 Prompt 编排、工具调用、状态管理等逻辑；
- 与 [[sandbox-isolation]] 协同，实现单次交互的资源与安全边界控制；
- 是 [[aliyun]] 提供的支撑 [[ai-agent]] 企业级落地的核心基础设施。
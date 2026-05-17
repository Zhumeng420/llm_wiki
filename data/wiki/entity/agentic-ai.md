---
title: "Agentic AI"
slug: "agentic-ai"
type: "entity"
summary: "一类具备自主目标分解、工具调用、环境交互与持续学习能力的 AI 系统，代表 AI 应用的新范式。"
sources: [https://developer.aliyun.com/article/1693206?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.4.24c76381WE5wrD]
tags: [AI范式,智能体,下一代AI]
updated_at: 2026-05-17T14:50:03.850225900Z
---

# Agentic AI

## 核心能力
- **感知（Perceive）**：理解用户输入与上下文；
- **规划（Plan）**：将目标拆解为子任务序列；
- **行动（Act）**：调用 API、检索知识、执行代码等；
- **反思（Reflect）**：评估结果并优化后续行为。

## 与 AgentRun 的关系
- [[agentrun]] 是专为 [[agentic-ai]] 设计的生产就绪运行时；
- 解决该范式在落地中面临的工程化断点，如长周期任务可靠性、多跳调用可观测性、安全边界缺失等；
- 推动 [[ai-agent-deployment]] 从实验走向企业核心业务系统。
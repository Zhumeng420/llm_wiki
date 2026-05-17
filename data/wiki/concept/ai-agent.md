---
title: "AI Agent"
slug: "ai-agent"
type: "concept"
summary: "具备感知、规划、工具调用与反思能力的自主决策式人工智能程序。"
sources: [https://developer.aliyun.com/article/1693206?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.4.24c76381WE5wrD]
tags: [大模型应用,智能体]
updated_at: 2026-05-17T14:44:38.955427500Z
---

# AI Agent

## 基本定义
AI Agent 是基于大语言模型（LLM）构建的、能自主完成复杂任务的软件实体，典型能力包括：
- 环境感知（读取输入/上下文）；
- 目标分解与任务规划；
- 工具调用（API、数据库、插件）；
- 自我反思与纠错。

## 与 AgentRun 的关系
- AgentRun 并非 AI Agent 框架本身，而是其在生产环境中的「运行容器」；
- 解决 AI Agent 在真实业务中面临的部署复杂度、稳定性、可观测性等挑战；
- 支持将各类 [[ai-agent]] 实现（如基于 LangChain 或自研框架）一键部署至生产环境。
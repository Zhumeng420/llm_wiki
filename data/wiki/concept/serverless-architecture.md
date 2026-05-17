---
title: "Serverless架构"
slug: "serverless-architecture"
type: "concept"
summary: "一种云计算模型，由云厂商完全托管基础设施，用户仅关注代码逻辑，按实际执行资源消耗付费。"
sources: [https://developer.aliyun.com/article/1693206?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.4.24c76381WE5wrD]
tags: [架构模式,云计算,弹性计算]
updated_at: 2026-05-17T14:50:03.452160300Z
---

# Serverless架构

## 定义特征
- 无服务器运维（No infrastructure management）；
- 自动弹性伸缩（Event-driven scaling）；
- 按执行时长与内存用量计费（Pay-per-use）；
- 冷启动与热执行并存。

## 在 AgentRun 中的作用
- 作为 [[agentrun]] 的底层执行基座，屏蔽集群调度与节点维护复杂度；
- 使 AI Agent 具备天然的突发流量应对能力，契合对话类、任务型 Agent 的请求模式；
- 与 [[aliyun-function-compute]] 深度协同，实现端到端 Serverless AI 工作流。
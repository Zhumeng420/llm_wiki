---
title: "Agentic AI"
slug: "agentic-ai"
type: "concept"
summary: "一种具备自主目标分解、工具调用、环境交互与持续学习能力的AI系统范式，区别于传统单次生成式AI。"
sources: [https://mp.weixin.qq.com/s/3noo6wAU1sfws2yJEVF47w]
tags: [paradigm,ai-architecture]
updated_at: 2026-05-17T14:11:57.418831200Z
---

# Agentic AI

## 定义与特征
Agentic AI指能自主规划、决策、调用工具、感知环境并迭代优化的AI系统，强调目标驱动、状态维持与任务闭环，是AI从‘回答问题’迈向‘解决问题’的关键演进。

## 关键支撑能力
- **有状态会话**：依赖[[会话亲和]]等机制维持上下文；
- **工具协同**：需[[MCP协议]]或Function Call等标准化接口；
- **弹性执行环境**：高度依赖[[Serverless]]架构实现按需调度与成本可控；
- **可观测性与治理**：要求[[全链路可观测]]、[[模型治理]]与[[工具治理]]保障稳定性与安全性。

## 实践载体
阿里云[[aliyun-function-compute-agentrun]]是当前面向企业生产环境最完整的Agentic AI基础设施平台之一。
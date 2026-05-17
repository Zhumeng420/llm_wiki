---
title: "AI Agent 架构概述"
slug: "ai-agent-architecture"
type: "concept"
summary: "一种面向动态交互场景设计的 AI Agent 系统架构，强调模块解耦、工具可插拔与状态可管理。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [架构,设计模式,动态交互]
updated_at: 2026-05-17T14:51:35.700242200Z
---

# AI Agent 架构概述

## 核心组件
- **Orchestrator（编排器）**：协调 LLM 推理、工具选择与结果聚合；
- **Tool Registry（工具库）**：注册并管理代码解释器、API 调用、数据库查询等能力；
- **State Manager（状态管理）**：维护会话历史、用户偏好与临时上下文；
- **Security Gateway（安全网关）**：集成 [[sandbox-isolation]] 对所有工具执行进行沙箱化约束。

## 与本文方案的关系
- 本文提出的 FC+Sandbox 方案，是该架构在阿里云环境下的具体实现路径；
- 所有组件均以函数形式部署于 [[aliyun-fc]]，天然契合该架构对弹性与隔离的要求。
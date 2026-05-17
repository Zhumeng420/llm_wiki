---
title: "AI Agent"
slug: "ai-agent"
type: "entity"
summary: "具备感知、决策、行动能力的智能体，本文特指基于函数计算与沙箱构建的企业级可部署 AI 应用。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [AI,智能体,应用]
updated_at: 2026-05-17T14:51:35.489193300Z
---

# AI Agent

## 定义与特征
- 不是静态模型 API，而是能理解上下文、调用工具、维护会话状态、响应动态输入的交互式系统；
- 本文聚焦于‘类 Chat Coding’场景（如自然语言生成代码、调试建议、文档生成等）。

## 构建要求
- **安全可控**：依赖 [[sandbox-isolation]] 实现执行隔离；
- **弹性可靠**：依托 [[aliyun-fc]] 实现按需扩缩与高可用；
- **架构可演进**：遵循 [[ai-agent-architecture]] 定义的动态交互范式，支持未来多模态与跨系统集成。
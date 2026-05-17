---
title: "Sandbox沙箱"
slug: "sandbox"
type: "entity"
summary: "一种轻量级运行时隔离环境，用于在共享基础设施中安全执行不可信或动态生成的代码。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [安全隔离,运行时防护,AI执行环境]
updated_at: 2026-05-17T14:42:18.931971900Z
---

# Sandbox沙箱

## 核心能力
- 进程/文件/网络/系统调用级隔离；
- 资源配额限制（CPU、内存、执行时长）；
- 支持动态加载与快速销毁。

## 在AI Agent架构中的定位
为 [[ai-agent-enterprise-deployment]] 提供可信执行边界，确保LLM生成代码（如 [[chat-coding]] 中的补全脚本）在受限环境中运行，防止越权操作或资源耗尽。

## 技术协同
- 与 [[function-compute-fc]] 结合，实现“单函数单沙箱”的部署范式；
- 是 [[sandbox-security]] 的具体实现载体。
---
title: "沙箱隔离"
slug: "sandbox-security"
type: "concept"
summary: "通过运行时环境隔离保障代码执行安全的技术机制，是企业级AI Agent可信执行的核心防线。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [安全机制,运行时防护,零信任执行]
updated_at: 2026-05-17T14:42:19.914092400Z
---

# 沙箱隔离

## 隔离层级
- 系统调用过滤（seccomp）；
- 文件系统挂载限制（chroot / overlayfs）；
- 网络策略（默认禁用外连，白名单管控）；
- 资源硬限（CPU时间片、内存上限、进程数）。

## 与AI Agent的关系
- 是 [[ai-agent-enterprise-deployment]] 不可妥协的安全前提；
- 在 [[sandbox]] 中具体实现，为LLM生成代码提供最小权限执行环境；
- 与 [[function-compute-fc]] 协同，形成“函数封装 + 沙箱加固”的双保险模型。
---
title: "Sandbox沙箱环境"
slug: "sandbox-environment"
type: "entity"
summary: "一种轻量级、强隔离的运行时环境，用于安全执行不可信代码。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [安全隔离,运行时防护,沙箱]
updated_at: 2026-05-17T14:41:21.449453300Z
---

# Sandbox沙箱环境

## 核心能力
- 进程/文件/网络/系统调用级隔离
- 资源配额限制（CPU、内存、执行时长）
- 不可逃逸、不可持久化设计

## 在AI Agent架构中的角色
- 为 AI Agent 的插件（Plugin）、工具函数（Tool Call）提供可信执行边界
- 防止恶意或错误代码影响主服务或其它Agent实例
- 与 [[aliyun-function-compute]] 深度集成，构成「Serverless + Sandbox」双控底座

## 安全意义
- 实现 [[sandbox-security]] 的工程化落地
- 是企业级 AI 可控性的关键基础设施
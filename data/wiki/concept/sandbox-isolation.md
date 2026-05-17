---
title: "沙箱隔离"
slug: "sandbox-isolation"
type: "concept"
summary: "一种运行时安全机制，通过资源限制与环境隔离确保代码执行互不干扰。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [安全,隔离,运行时]
updated_at: 2026-05-17T14:51:35.299201600Z
---

# 沙箱隔离

## 技术原理
- 利用轻量虚拟化或容器级隔离，在单台宿主机上为每个函数实例分配独立 CPU、内存、文件系统视图；
- 阻断进程间通信、网络直连与磁盘共享，防止恶意或异常代码越界。

## 对 AI Agent 的价值
- 解决 AI Agent 动态加载第三方工具/插件时的安全风险；
- 支持多租户、多用户并发调用同一 Agent 而互不影响；
- 是 [[aliyun-fc]] 实现企业级可信 AI 应用的关键安全底座，直接赋能 [[ai-agent]] 落地。
---
title: "阿里云函数计算"
slug: "aliyun-function-compute"
type: "entity"
summary: "阿里云提供的 Serverless 计算服务，支持事件驱动、弹性伸缩的代码执行，是 AgentRun 的底层运行平台。"
sources: [https://developer.aliyun.com/article/1693206?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.4.24c76381WE5wrD]
tags: [Serverless,云计算,阿里云]
updated_at: 2026-05-17T14:50:03.041934900Z
---

# 阿里云函数计算

## 基本定位
阿里云函数计算（Function Compute）是阿里云推出的全托管 Serverless 计算服务，用户无需管理服务器即可运行代码。

## 与 AgentRun 的关系
- AgentRun 是构建于 [[aliyun-function-compute]] 之上的专用运行时服务；
- 复用其自动扩缩容、按量付费、事件触发等 [[serverless-architecture]] 能力；
- 提供更高层抽象，专为 AI Agent 生命周期管理设计。

## 技术特性
- 全托管基础设施；
- 内置日志、监控与追踪能力；
- 支持多种语言与自定义容器运行时。
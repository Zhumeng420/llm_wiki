---
title: "函数计算FC"
slug: "function-compute-fc"
type: "entity"
summary: "阿里云提供的Serverless函数即服务（FaaS）平台，支持事件驱动、免运维、自动伸缩的代码执行环境。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [Serverless,阿里云,FaaS]
updated_at: 2026-05-17T14:42:18.702462100Z
---

# 函数计算FC

## 基本特性
- 事件驱动：支持HTTP、消息队列、对象存储等多种触发源；
- 免运维：用户仅关注代码逻辑，底层资源由平台自动调度；
- 自动伸缩：毫秒级冷启动与弹性扩缩容能力。

## 在AI Agent中的作用
作为 [[ai-agent-enterprise-deployment]] 的执行基座，为每个Agent任务提供独立、隔离、按需分配的运行单元，与 [[sandbox]] 协同实现安全可控的推理与执行闭环。

## 关联生态
- 托管于 [[aliyun]]；
- 是 [[serverless-architecture]] 的典型实现；
- 与 [[sandbox]] 深度集成，形成“函数粒度+沙箱边界”的双重隔离模型。
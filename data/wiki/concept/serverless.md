---
title: "Serverless"
slug: "serverless"
type: "concept"
summary: "一种云计算范式，开发者专注业务逻辑，基础设施自动伸缩与运维。"
sources: [https://developer.aliyun.com/article/1681979?spm=5176.30385154.J_0m1BZJhr4lWRrkFCF4I_N.2.24c76381WE5wrD]
tags: [架构范式,云原生,弹性计算]
updated_at: 2026-05-17T14:50:52.064914700Z
---

# Serverless

## 核心特征
- 无服务器管理；
- 按需计费；
- 事件驱动、自动扩缩容。

## 与AI Agent的契合点
- 匹配 AI Agent 的突发性、会话式负载特征；
- 降低企业自建推理服务的运维复杂度；
- 结合 [[sandbox-isolation]]，兼顾弹性与安全。

## 典型载体
- [[aliyun-function-compute-fc]] 是其在阿里云的代表性实现。
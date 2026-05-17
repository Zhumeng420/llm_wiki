---
title: "阿里云函数计算AgentRun"
slug: "aliyun-function-compute-agentrun"
type: "entity"
summary: "阿里云推出的基于Serverless架构的企业级Agentic AI基础设施平台，支持毫秒级弹性、会话亲和、安全沙箱与全链路可观测。"
sources: [https://mp.weixin.qq.com/s/3noo6wAU1sfws2yJEVF47w]
tags: [platform,serverless,agentic-ai]
updated_at: 2026-05-17T14:11:57.350893700Z
---

# 阿里云函数计算AgentRun

## 定位与价值
阿里云函数计算AgentRun是面向生产环境的Agentic AI基础设施平台，以函数计算FC为技术底座，融合Serverless特性与AI原生需求，显著降低TCO（平均降幅60%），加速AI Agent规模化落地。

## 五大核心能力
### 毫秒级极致弹性
- 支持0→百万级并发自动伸缩；
- 浅休眠（1ms唤醒，百倍加速）与深休眠（秒级唤醒+会话状态持久化）并存。

### 会话亲和
- 同一会话精准路由至同一实例，保障多轮对话上下文连续性，突破传统Serverless无状态瓶颈。

### 企业级安全沙箱
- 提供Code Interpreter、Browser Tool、All In One三类沙箱；
- 多语言执行引擎 + 浏览器自动化引擎；
- 请求/实例/会话三级算力隔离 + 动态挂载 + 细粒度权限控制 + 完整审计日志。

### 模型与工具统一治理
- **模型治理**：统一模型代理、熔断、多模型Fallback、负载均衡、内容审核与敏感信息过滤；
- **工具治理**：MCP协议标准化封装 + Function Call双协议支持，含Hook机制、智能路由、语义分析扩展能力。

### 全链路可观测
- 追踪请求→响应全过程，可视化展示意图理解、模型推理、工具调用、知识检索各环节状态与耗时，支撑精准性能优化。

## 生态集成
- 开发框架：[[AgentScope]]、[[Langchain]]、[[ADK]]、[[CrewAI]]；
- 开源组件：[[LiteLLM]]、[[Mem0]]、[[RagFlow]]；
- 客户与场景：[[百炼]]、[[魔搭社区]]、[[吉利汽车]]、[[Z.ai]]。
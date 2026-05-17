# LLM-Wiki

> 一个由 **LLM 自动构建**的个人知识库系统。  
> 投喂任意文档、网页或远端源 —— 一键得到带交叉引用的 Wiki、知识图谱、混合检索，以及独家的 **主动空白反推**。

**简体中文** | [English](README.md)

---

## ✨ 核心亮点

- **多格式数据源** —— PDF / Word / Excel / PPT / 图片（OCR）/ Markdown / 纯文本 / URL 网页 / 飞书 / 钉钉。
- **LLM 驱动的 Wiki 生成** —— 自动解析、总结、打标签，并产出带 `[[wikilinks]]` 的 Markdown 页面。
- **跨源实体归一化** —— 内置 EntityAliasService，导入时即合并同义实体，图谱不再碎片化。
- **知识图谱 + Louvain 社区发现** —— AntV G6 v5 力导向可视化，自动识别桥节点 / 孤立节点。
- **混合检索** —— Lucene BM25 + 向量 KNN，经 RRF 融合后再用图谱邻接加权。
- **主动空白反推** —— 系统主动猜测用户可能问的问题，自我检索 + LLM 判定能否回答，**精确告知缺失什么知识、应该补充什么资料**。
- **定时刷新** —— Quartz Cron 自动重抓订阅源。
- **评测体系** —— 上传 CSV 问题集，输出检索 / 回答指标，可选 LLM-as-judge。
- **零运维本地部署** —— 单 Spring Boot fat-jar + 内嵌 H2 + 本地 Lucene 索引。

---

## 🧱 系统架构

```
┌────────────────┐   ┌──────────────┐   ┌────────────────────┐
│   数据源       │──▶│   解析器      │──▶│   LLM 生成器        │
│ 文件 / 网页    │   │ PDF/POI/JS   │   │ 摘要/链接/标签      │
│ 飞书 / 钉钉    │   │ OCR / 爬取   │   │ + 别名归一化        │
└────────────────┘   └──────────────┘   └─────────┬──────────┘
                                                  │
                          ┌───────────────────────┼───────────────────────┐
                          ▼                       ▼                       ▼
                   ┌────────────┐         ┌────────────┐          ┌────────────┐
                   │  Wiki 页面 │         │  知识图谱   │          │   Lucene   │
                   │ (H2 + MD)  │         │ (Louvain)  │          │ BM25 + KNN │
                   └─────┬──────┘         └──────┬─────┘          └──────┬─────┘
                         └─────────────┬─────────┴────────┬──────────────┘
                                       ▼                  ▼
                                ┌──────────────┐   ┌──────────────────┐
                                │  混合检索    │   │  主动空白反推     │
                                └──────┬───────┘   └────────┬─────────┘
                                       ▼                    ▼
                            ┌─────────────────────────────────────┐
                            │  Vue 3 + Element Plus + AntV G6     │
                            └─────────────────────────────────────┘
```

### 技术栈

| 层级    | 技术                                                                            |
| ------- | ------------------------------------------------------------------------------- |
| 后端    | Java 17、Spring Boot 3.3.5、Spring Data JPA、Quartz                              |
| 存储    | H2（文件模式）+ 本地文件系统（raw / wiki / index / graph）                       |
| 解析    | Apache PDFBox 3、POI 5、Tika 2、Jsoup、Readability4J                             |
| 检索    | Apache Lucene 9.11（BM25 + KnnFloatVectorQuery）+ RRF                            |
| 图谱    | JGraphT + 自研 Louvain 社区发现                                                  |
| LLM     | OpenAI 兼容的 Chat / Embedding（默认 DashScope / Qwen）                          |
| 前端    | Vue 3、Vite、TypeScript、Element Plus、AntV G6 v5、Pinia、Vue Router             |

---

## 🚀 快速开始

### 前置依赖
- JDK 17+
- Node.js 18+
- 任意 OpenAI HTTP 协议兼容的 LLM 服务（DashScope / OpenAI / Azure / vLLM / Ollama 等）

### 1. 克隆并配置

```bash
git clone https://github.com/<you>/llm-wiki.git
cd llm-wiki
```

打开 `src/main/resources/application.yml` 填写自己的 key，**或**通过环境变量覆盖：

```bash
export LLM_WIKI_CHAT_API_KEY=sk-xxxxxxxx
export LLM_WIKI_CHAT_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_WIKI_CHAT_MODEL=qwen-plus

export LLM_WIKI_EMBED_API_KEY=sk-xxxxxxxx
export LLM_WIKI_EMBED_MODEL=text-embedding-v4
```

> ⚠️ **安全提示**：**切勿**把真实 API Key 提交到仓库。生产环境请使用环境变量或 Spring profile。如果你 fork 自一个曾经包含明文 key 的提交，请立刻 **吊销那把 key**。

### 2. 构建并启动后端

```bash
./mvnw -DskipTests package
java -jar target/llm-wiki-0.0.1-SNAPSHOT.jar
```

后端监听 **http://localhost:8080**，H2 控制台位于 `/h2-console`。

> ℹ️ H2 数据文件位于 `./data/db/llmwiki.mv.db`（**相对当前工作目录**）。请始终从同一目录（通常是项目根）启动 jar，否则可能读到空库。

### 3. 启动前端

```bash
cd web
npm install
npm run dev
```

访问 **http://localhost:5173** —— Vite 会自动把 `/api/*` 代理到后端。

---

## 🗺️ 功能模块

| 路由         | 页面         | 功能描述                                                                                    |
| ------------ | ------------ | ------------------------------------------------------------------------------------------- |
| `/dashboard` | 总览         | 计数、最近导入事件、LLM 连接状态                                                              |
| `/sources`   | 数据源       | 上传文件 / 提交 URL / 飞书 / 钉钉；任务队列实时状态，可重试/取消                              |
| `/wiki`      | Wiki 页面    | 浏览生成的 Markdown 页面，支持 `[[wikilink]]` 跳转                                            |
| `/graph`     | 知识图谱     | G6 v5 力导向图、Louvain 社区染色、桥节点/孤立节点分析、一键 **重建图谱**                       |
| `/search`    | 智能检索     | BM25 + KNN 混合检索 + 图谱加权                                                                |
| `/insights`  | 空白反推     | ① 结构 + 语义空白审计 ② **主动空白反推**（详见下文）                                          |
| `/schedule`  | 定时更新     | Cron 表达式调度，自动重新抓取监听中的源                                                       |
| `/eval`      | 评测体系     | 上传问答 CSV，输出检索 / 回答指标，可选 LLM-as-judge                                          |
| `/settings`  | 系统设置     | 编辑并测试 LLM 端点、切换模型                                                                 |

---

## 🤖 主动空白反推

大多数"知识空白"工具只能被动响应用户提问。LLM-Wiki 更进一步：

1. **问题生成** —— LLM 读取你的 wiki 概览、标签、桥节点，**主动从 9 个角度**（是什么 / 怎么做 / 为什么 / 对比 / 最佳实践 / 常见坑 / 集成 / 性能 / 安全）猜测真实用户最可能问的具体问题。
2. **自我检索回答** —— 每个候选问题走混合检索，取 top-K 证据。
3. **判定** —— LLM 评估每个问题为 `answerable` / `partial` / `no`，并列出缺失的信息点。
4. **聚合建议** —— 按词频排序，**精确告知你应该补充的资料/文档**。

```bash
curl "http://localhost:8080/api/insights/proactive-gap?count=15&topK=5"
```

---

## 📡 REST API（节选）

| 方法 | 路径                              | 说明                                  |
| ---- | --------------------------------- | ------------------------------------- |
| POST | `/api/sources/file`               | 上传文件（multipart）                  |
| POST | `/api/sources/url`                | 提交网页 URL                          |
| GET  | `/api/wiki/pages`                 | 列出 Wiki 页面（可选 `?type=`）        |
| GET  | `/api/graph`                      | 获取完整图谱（`?minWeight=`）          |
| POST | `/api/graph/rebuild`              | 重新归一化实体并重建图谱                |
| GET  | `/api/search?q=&topK=`            | 混合检索                              |
| GET  | `/api/insights/gap`               | 结构 + 语义空白报告                    |
| GET  | `/api/insights/proactive-gap`     | 主动空白反推                          |
| POST | `/api/eval/run`                   | 执行评测集                            |

---

## 📁 项目结构

```
.
├── src/main/java/com/example/llmwiki/
│   ├── api/             REST 控制器
│   ├── ingest/          导入流水线、prompt、队列 worker
│   ├── parser/          PDF / Office / OCR / URL 解析器
│   ├── llm/             OpenAI 兼容的 Chat & Embedding 客户端
│   ├── retrieval/       Lucene 索引 + 混合检索
│   ├── graph/           GraphService、EntityAliasService、Louvain
│   ├── insight/         GapAnalyzer、ProactiveQuestionAnalyzer
│   ├── scheduler/       Quartz 任务
│   └── eval/            CSV 驱动的评测框架
├── src/main/resources/
│   ├── application.yml
│   └── prompts/         analyze.md / generate.md / gap.md / proactive_questions.md / answerability.md
├── web/                 Vue 3 前端
└── data/                运行时：raw / wiki / index / graph / db
```

---

## 🛠️ 配置参考

所有配置项均位于 `application.yml` 的 `llm-wiki:` 命名空间下：

```yaml
llm-wiki:
  storage:
    root-dir: ./data        # 所有运行时数据
  llm:
    chat:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1   # 必须截止到 /v1
      model: qwen-plus
      json-mode: true
    embedding:
      model: text-embedding-v4
      dimensions: 1024       # 必须与 Lucene 索引维度一致；修改后需重建索引
  scheduler:
    enabled: true
    cron: "0 0 3 * * ?"      # 每天凌晨 3 点
  ingest:
    worker-threads: 1
```

---

## 🤝 贡献

欢迎提交 PR 与 Issue：
1. 提交前请运行 `./mvnw verify` 和 `npm run build`。
2. 后端代码遵循 **《阿里巴巴 Java 开发手册》**。
3. Prompt 模板（`src/main/resources/prompts/*.md`）保持语言中立，由系统指令强制中文输出。

---

## 📄 许可证

本项目基于 [MIT License](./LICENSE) 开源。

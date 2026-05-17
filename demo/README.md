# LLM-Wiki

> A **self-building personal knowledge base** powered by LLMs.  
> Drop in any document, web page or remote source — get a fully cross-linked Wiki, a knowledge graph, hybrid search and **proactive knowledge-gap detection** out of the box.

[简体中文](./README.zh-CN.md) | **English**

---

## ✨ Highlights

- **Multi-format ingest** — PDF / Word / Excel / PPT / images (OCR) / Markdown / Plain text / URLs / Feishu / DingTalk.
- **LLM-driven Wiki generation** — every source is parsed, summarized, tagged and turned into a Markdown page with `[[wikilinks]]`.
- **Cross-source entity normalization** — an alias service merges synonymous entities at ingest time so the graph never fragments.
- **Knowledge graph + Louvain community detection** — interactive force-directed view (AntV G6 v5) with bridge-node / isolated-node analytics.
- **Hybrid retrieval** — Lucene BM25 + vector KNN fused via Reciprocal Rank Fusion, boosted by graph adjacency.
- **Proactive gap analysis** — the system actively guesses the questions a user *might* ask, self-answers them via retrieval + LLM, and tells you **exactly what knowledge is missing and what to add**.
- **Scheduled refresh** — Quartz-based watcher re-ingests changed sources on a cron.
- **Evaluation harness** — upload a CSV question set, get retrieval / answer metrics with optional LLM-as-a-judge.
- **Zero-ops local deployment** — single Spring Boot fat-jar + embedded H2 + local Lucene index.

---

## 🧱 Architecture

```
┌────────────────┐   ┌──────────────┐   ┌────────────────────┐
│  Sources       │──▶│  Parsers     │──▶│  LLM Generator     │
│  files / URL   │   │  PDF/POI/JS  │   │  summary/links/tag │
│  feishu/dingtk │   │  OCR/Crawl   │   │  + alias canonical │
└────────────────┘   └──────────────┘   └─────────┬──────────┘
                                                  │
                          ┌───────────────────────┼───────────────────────┐
                          ▼                       ▼                       ▼
                   ┌────────────┐         ┌────────────┐          ┌────────────┐
                   │ Wiki Pages │         │ Graph      │          │ Lucene     │
                   │  (H2 + MD) │         │ (Louvain)  │          │ BM25 + KNN │
                   └─────┬──────┘         └──────┬─────┘          └──────┬─────┘
                         └─────────────┬─────────┴────────┬──────────────┘
                                       ▼                  ▼
                                ┌──────────────┐   ┌──────────────────┐
                                │  Hybrid      │   │  Proactive Gap   │
                                │  Search      │   │  Analyzer        │
                                └──────┬───────┘   └────────┬─────────┘
                                       ▼                    ▼
                            ┌─────────────────────────────────────┐
                            │   Vue 3 + Element Plus + AntV G6    │
                            └─────────────────────────────────────┘
```

### Tech stack

| Layer       | Tech                                                                  |
| ----------- | --------------------------------------------------------------------- |
| Backend     | Java 17, Spring Boot 3.3.5, Spring Data JPA, Quartz                   |
| Storage     | H2 (file mode), local FS for raw / wiki / index / graph               |
| Parsing     | Apache PDFBox 3, POI 5, Tika 2, Jsoup, Readability4J                  |
| Search      | Apache Lucene 9.11 (BM25 + KnnFloatVectorQuery), RRF fusion           |
| Graph       | JGraphT + custom Louvain implementation                               |
| LLM         | OpenAI-compatible Chat & Embedding (DashScope / Qwen by default)      |
| Frontend    | Vue 3, Vite, TypeScript, Element Plus, AntV G6 v5, Pinia, Vue Router  |

---

## 🚀 Quick Start

### Prerequisites
- JDK 17+
- Node.js 18+
- An LLM endpoint compatible with the OpenAI HTTP schema (e.g. DashScope, OpenAI, Azure, vLLM, Ollama).

### 1. Clone and configure

```bash
git clone https://github.com/<you>/llm-wiki.git
cd llm-wiki
```

Open `src/main/resources/application.yml` and set your own keys, **or** override them at runtime:

```bash
export LLM_WIKI_CHAT_API_KEY=sk-xxxxxxxx
export LLM_WIKI_CHAT_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_WIKI_CHAT_MODEL=qwen-plus

export LLM_WIKI_EMBED_API_KEY=sk-xxxxxxxx
export LLM_WIKI_EMBED_MODEL=text-embedding-v4
```

> ⚠️ **Security**: never commit a real API key. Use environment variables or Spring profiles in production.

### 2. Build & run the backend

```bash
./mvnw -DskipTests package
java -jar target/llm-wiki-0.0.1-SNAPSHOT.jar
```

Backend starts on **http://localhost:8080** with H2 console at `/h2-console`.

> ℹ️ The H2 file lives at `./data/db/llmwiki.mv.db` **relative to your current working directory**. Always launch the jar from the same directory (typically the project root) to keep your data.

### 3. Run the frontend

```bash
cd web
npm install
npm run dev
```

Open **http://localhost:5173** — Vite proxies `/api/*` to the backend automatically.

---

## 🗺️ Modules (UI menu)

| Route        | Page             | What it does                                                                                   |
| ------------ | ---------------- | ---------------------------------------------------------------------------------------------- |
| `/dashboard` | Overview         | Counters, recent ingest events, LLM connectivity status                                         |
| `/sources`   | Data sources     | Upload files / submit URL / Feishu / DingTalk; live task queue with retry & cancel             |
| `/wiki`      | Wiki pages       | Browse generated Markdown pages, follow `[[wikilinks]]`                                        |
| `/graph`     | Knowledge graph  | Force-directed graph (G6 v5), Louvain communities, isolated / bridge nodes, **Rebuild** button |
| `/search`    | Smart search     | Hybrid BM25 + KNN search with graph boost                                                      |
| `/insights`  | Knowledge gaps   | ① Structural & semantic gap audit ② **Proactive gap reasoning** (see below)                    |
| `/schedule`  | Scheduled jobs   | Cron-based re-ingest of watched sources                                                        |
| `/eval`      | Evaluation       | Upload a Q-set CSV → retrieval / answer metrics + LLM-as-judge                                 |
| `/settings`  | Settings         | Edit & ping LLM endpoints, change models                                                       |

---

## 🤖 Proactive Gap Analysis

Most "knowledge gap" tools only react to user questions. LLM-Wiki goes further:

1. **Question synthesis** — the LLM reads your wiki overview, tags and bridge nodes, then **invents the questions a real user is likely to ask** across 9 angles: what / how / why / compare / best_practice / pitfall / integration / perf / security.
2. **Self-answer via retrieval** — every candidate is fed through the hybrid searcher to gather top-K evidence.
3. **Verdict** — the LLM judges each question as `answerable` / `partial` / `no` and lists the missing information points.
4. **Aggregated suggestions** — frequency-ranked list of *exact* documents / sources you should add next.

```bash
curl "http://localhost:8080/api/insights/proactive-gap?count=15&topK=5"
```

---

## 📡 REST API (subset)

| Method | Path                              | Description                                  |
| ------ | --------------------------------- | -------------------------------------------- |
| POST   | `/api/sources/file`               | Upload a file (multipart)                    |
| POST   | `/api/sources/url`                | Submit a web URL                             |
| GET    | `/api/wiki/pages`                 | List wiki pages (optional `?type=`)          |
| GET    | `/api/graph`                      | Get full graph (`?minWeight=`)               |
| POST   | `/api/graph/rebuild`              | Re-canonicalize entities and rebuild graph   |
| GET    | `/api/search?q=&topK=`            | Hybrid search                                |
| GET    | `/api/insights/gap`               | Structural + semantic gap report             |
| GET    | `/api/insights/proactive-gap`     | Proactive gap reasoning                      |
| POST   | `/api/eval/run`                   | Run an evaluation set                        |

---

## 📁 Project layout

```
.
├── src/main/java/com/example/llmwiki/
│   ├── api/             REST controllers
│   ├── ingest/          IngestPipeline, prompts, queue worker
│   ├── parser/          PDF / Office / OCR / URL parsers
│   ├── llm/             OpenAI-compatible Chat & Embedding clients
│   ├── retrieval/       Lucene indexer + HybridSearcher
│   ├── graph/           GraphService, EntityAliasService, Louvain
│   ├── insight/         GapAnalyzer, ProactiveQuestionAnalyzer
│   ├── scheduler/       Quartz jobs
│   └── eval/            CSV-driven evaluation harness
├── src/main/resources/
│   ├── application.yml
│   └── prompts/         analyze.md / generate.md / gap.md / proactive_questions.md / answerability.md
├── web/                 Vue 3 frontend
└── data/                runtime: raw / wiki / index / graph / db
```

---

## 🛠️ Configuration reference

All keys live under `llm-wiki:` in `application.yml`. Notable ones:

```yaml
llm-wiki:
  storage:
    root-dir: ./data        # all runtime files
  llm:
    chat:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1   # must end at /v1
      model: qwen-plus
      json-mode: true
    embedding:
      model: text-embedding-v4
      dimensions: 1024       # must match index; rebuild if you change it
  scheduler:
    enabled: true
    cron: "0 0 3 * * ?"      # daily 03:00
  ingest:
    worker-threads: 1
```

---

## 🤝 Contributing

PRs and issues are very welcome. Please:
1. Run `./mvnw verify` and `npm run build` before submitting.
2. Follow the **Alibaba Java Coding Standards** for backend code.
3. Keep prompts (`src/main/resources/prompts/*.md`) language-agnostic — the system instruction enforces 中文 output.

---

## 📄 License

This project is released under the [MIT License](./LICENSE).

# IBIOS — AI Agent 协作指南

> 本文件是 AI coding agent（Qoder、Cursor、Copilot 等）进入 IBIOS 项目时的第一份参考。
> 读完本文件后再读 `docs/` 下的四份设计文档和 `.specify/memory/constitution.md`。

---

## 项目定位

IBIOS 是一个 **企业级 Agent OS**——运行和管理 AI Agent 的统一底座。装在用户自己的机器上，向上为各类业务 Agent（运维、客服、HR、销售等）提供统一运行环境，向下接入 LLM、工具、记忆、渠道和审计。

核心价值：**私有部署、完全可审计、Java 原生、数据不出企业**。

---

## 技术栈（不可协商）

| 项 | 选择 |
|---|------|
| JDK | **21**（最低且目标版本） |
| 框架 | **Spring Boot 3.x** |
| LLM 调用 | **Spring AI Alibaba**（只用 Provider 抽象 + 协议转换 + @Tool schema 生成） |
| 构建 | **Maven 多模块**（9 个模块） |
| 持久化 | **SQLite + Spring Data JPA**（扩展阶段加 PostgreSQL） |
| 数据库迁移 | **Flyway**（`db/migration/{vendor}/` 双轨目录） |
| CLI | **Picocli** |
| YAML 解析 | **SnakeYAML** |
| 日志 | **Logback + SLF4J**（结构化日志） |
| 监控 | **Micrometer + Prometheus** |

---

## Maven 模块结构

```
ibios/
├── ibios-core          # 核心抽象：Profile、IbiosTool、ReActLoop、PromptBuilder、ToolExecutor、Sandbox
├── ibios-provider      # 核心能力一：LLM Provider 抽象、多 Provider 路由、ToolSchema 翻译
├── ibios-storage       # 持久化：Session、LlmCall、ToolInvocation 的 JPA 实体与 Repository
├── ibios-memory        # 核心能力三：MemoryService 三层门面、LongTermMemory、MemoryTools
├── ibios-tool          # 核心能力四：内置 Tool、MCP Client、ToolRegistry、Sandbox 实现
├── ibios-web           # 核心能力五：REST API、Web Server
├── ibios-cli           # Picocli 命令行（ibios init / chat 等 12 个命令）
├── ibios-channel-cli   # CLI Channel 接入
└── ibios-boot          # Spring Boot 启动模块，打出可执行 JAR
```

**依赖方向**：`ibios-boot` → 所有模块 → `ibios-core`（core 不依赖任何 Spring 框架，只有 SnakeYAML）。

---

## 五大核心能力与推进顺序

| User Story | 能力 | 依赖 | 状态 |
|-----------|------|------|------|
| US-1 | LLM Provider 抽象 | 无 | **Lesson 16 已完成** |
| US-2 | ReAct 循环 | US-1 | 下一步（Lesson 17） |
| US-3 | Memory 三层记忆 | US-2 | 待开始 |
| US-4 | Plugin Tool 体系 | US-2 | 待开始 |
| US-5 | Web Service | US-1~4 | 待开始 |

推进顺序：**US-1 → US-2 → (US-3 ∥ US-4) → US-5**

---

## Constitution 红线（最常踩的坑）

以下 10 条原则是 **non-negotiable** 的，完整定义见 `.specify/memory/constitution.md`。这里列出最容易被 AI agent 写错的几条：

### 1. Spring AI 只用一半（原则四，最高频错误）

Spring AI **只用**：
- Provider 抽象和协议转换（向各家 LLM 的格式适配）
- `@Tool` 注解的 JSON Schema 生成

Spring AI **禁用**：
- 自动 tool 执行（`autoExecuteTools` 必须为 false）
- Agent / AgentExecutor 抽象
- ChatClient 的 tool-calling pipeline

**原因**：如果不禁用，tool 会被调两次（Spring AI 一次、IBIOS ToolExecutor 一次），且绕过 Sandbox 检查。

### 2. 自实现 ReAct 循环（原则三）

`ReActLoop` + `ToolExecutor` + `PromptBuilder` 全部在 `ibios-core` 自己实现。不依赖 Spring AI 的 Agent 抽象。

### 3. 显式 Provider 映射（原则九）

多 Provider 并存时，必须维护 `Map<String, ChatModel>` 显式映射（按 provider name），**不能靠类型扫描**。多个 ChatModel Bean 类型相同，扫描无法区分。

### 4. 审计 Day One（原则八）

`tool_invocations` 和 `llm_calls` 两张表核心阶段就写入，不只是日志。成功和失败都要记录。

### 5. 无明文密钥（原则十）

`grep -r "sk-"` 在代码和配置里找不到任何东西。API key 全部走 `${ENV_VAR}` 占位符。

### 6. 同步 + 虚拟线程（原则六）

不用 Reactor（Mono/Flux）、不用 CompletableFuture、不自建线程池。Spring MVC + virtual threads。

---

## 开发工作流

### Spec-Kit 流程

每个 User Story 的开发走 Spec-Kit 标准流程：

```
/speckit-specify → /speckit-clarify → /speckit-plan → /speckit-tasks → /speckit-analyze → /speckit-implement
```

所有 speckit 产出的内容（spec、plan、tasks、data-model、contracts、research）**必须用简体中文**。

### 课件驱动开发

本项目用 `/ibios-dev N` 技能驱动第 N 节课的开发，自动串联 specify → clarify → plan → tasks → implement → 验收。

### 构建与验证

```bash
# 编译 + 跑单测（排除 @Tag("integration")）
mvn clean verify

# 只跑某个模块的测试
mvn test -pl ibios-provider

# 格式化代码（Google Java Format + 阿里 P3C）
mvn spotless:apply

# 跑集成测试（需要环境变量）
DASHSCOPE_API_KEY=xxx mvn verify -Dgroups=integration
```

**实现完成的定义**：`mvn clean verify` 全绿。

---

## 代码规范

### Java 语法禁区

避开 P3C / ASM 解析不了的 Java 18+ 语法：
- **不用**增强 switch 的 `default ->` 写法（用传统 `switch-case-break`）
- **不用** text block 的嵌套缩进边界写法
- 静态检查是构建门禁，过不了就编译不过

### 测试规范

- 测试方法名用**英文**（P3C 要求）
- 单测默认跑，集成冒烟打 `@Tag("integration")` CI 跳过
- 每个 constitution 原则至少一个回归测试锚定

### 模块依赖规则

- `ibios-core` **不依赖** Spring 框架（只有 SnakeYAML）
- 跨模块依赖单向，**不允许循环依赖**
- `ibios-provider` 依赖 `ibios-core` + `ibios-storage`
- `ibios-storage` 依赖 `ibios-core`

---

## 关键文件索引

| 文件 | 作用 |
|------|------|
| `docs/IndustryResearch.md` | 业界调研：为什么做 |
| `docs/DemandAnalysis.md` | 需求文档：做什么 |
| `docs/TechnicalSolution.md` | 技术方案：怎么做 |
| `docs/AiProgrammingGuide.md` | 编程指南：怎么实施 |
| `.specify/memory/constitution.md` | 非协商原则（10 条） |
| `specs/001-agent-provider/` | Lesson 16 的 spec/plan/tasks/data-model/contracts |
| `ibios-boot/src/main/resources/schema.sql` | SQLite 建表脚本 |
| `ibios-boot/src/main/resources/application.yaml` | 应用配置 |

---

## 架构要点

### 整体分层

```
接入层（CLI Channel / Web Service / AgentScheduler）
    ↓
引擎层（ReActLoop / PromptBuilder / ToolExecutor）
    ↓
能力层（Provider / Memory / Tool）
    ↓
基础层（Profile 加载 / Session 存储 / SQLite / 配置加载）
```

### 三个触发入口

1. **CLI Channel** — 本地交互调试
2. **Web Service** — REST API 对外暴露
3. **AgentScheduler** — cron 定时触发

三个入口的消息汇入同一个引擎：`AgentService.process(Session, String)`。

### Agent 定义方式

一个 Agent = 一个目录 `.ibios/agents/<name>/`：
- `AGENT.md` = frontmatter（运行配置）+ 任务说明正文
- 可选 `skills/` 软连接、`scripts/*.py`、`REFERENCE.md`

Agent 目录是**上下文来源**，不是 Tool。`AgentLoader`/`ContextLoader` 归 `ibios-core`。

---

## 当前进度

- **Lesson 16（US-1 Agent Provider）**：✅ 26 个 task 全部完成，15 个单测全绿，`mvn clean verify` 通过
- **Lesson 17（US-2 ReAct 循环）**：下一步

---

## 验收 Demo（核心阶段必须跑通）

| Demo | 场景 | 涉及能力 |
|------|------|---------|
| Demo 一 | 每日天气（裸 AGENT.md） | Provider + ReAct + HTTP Tool |
| Demo 二 | 每日科技日报（AGENT.md + Skill 绑定） | + Memory + Skill 加载 |
| Demo 三 | 每日 GitHub 日报（AGENT.md + scripts/） | + MCP + Shell Tool |

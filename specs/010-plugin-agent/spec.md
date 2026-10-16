# Feature Specification: 插件化 Agent——一个目录定义一个会自己跑的 Agent

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: 插件化 Agent——一个目录定义一个会自己跑的 Agent
- **Lesson**: 29
- **Author**: IBIOS Dev
- **Date**: 2026-10-16
- **Status**: draft

---

## 1. Background & Value

到第 28 节为止，IBIOS 底座（内核）全部就位——Provider、ReAct、内置 Tool、Memory、Sandbox、定时、Web。但此刻想跑一个业务 Agent，还得把指令硬写进 Profile YAML 配置、手工拼装。**缺的正是那套"定义一个 Agent、把它装上去、让它跑起来"的标准机制。** 本节补上这块：往 `.ibios/agents/` 丢一个目录，它就出现在 Agent 列表里、到点自己跑、改指令即时生效——全程不写一行 Java、不动底座。从这一节起，IBIOS 才从"一个能跑 Agent 的框架"变成"一台能跑 N 个 Agent 的 OS"。

## 2. User Scenarios

### Scenario 1: 手工放一个 Agent 目录，系统启动自动注册

业务方在 `.ibios/agents/daily-reconcile/` 放一个目录（`AGENT.md` + `scripts/reconcile.py` + `skills/report-format.md` + `REFERENCE.md`）。系统启动时 `AgentLoader` 扫描 `.ibios/agents/`，解析 `AGENT.md` frontmatter 派生出 `Profile`，注册到 `ProfileRegistry`，有 `schedules` 的交给 `AgentScheduler`。`ibios profile list` / `GET /api/v1/profiles` 里出现这个 Agent。

### Scenario 2: Agent 目录的渐进式披露

Agent 被触发时，`AGENT.md` 正文进 system prompt（`ContextLoader` 注入）。目录里的 `skills/`、`REFERENCE.md`、`scripts/` 不预载——正文指引模型用底座既有的 `read_file` 读参考/子指令、用 `shell` 跑脚本，脚本产出进上下文、代码不进。

### Scenario 3: 运行时注册（为第 30 节铺路）

`ProfileRegistry` 改为可变并发 Map，新增 `remove(String)` / `exists(String)` 方法。`AgentScheduler` 抽出 `registerProfile(Profile)` 方法 + `scheduledTasks` 句柄表。启动扫描和运行时新增走同一段代码、同一套校验。

## 3. Functional Requirements

- **FR1**: `AgentLoader` 类在 `ibios-core`，解析 `.ibios/agents/<name>/AGENT.md`：拆出 frontmatter（YAML 配置）与正文（任务指令），识别 `scripts/`、`skills/`、`REFERENCE.md` 资源路径。
- **FR2**: `deriveProfile(Path agentDir) → Profile` 方法把 frontmatter 各字段映射到 `Profile` 值对象（name / description / identity / provider / tools / notifyChannels / schedules / bootstrap / settings / maxIterations / maxHistoryTurns）。
- **FR3**: 启动时扫描 `.ibios/agents/` 下所有子目录，对每个目录执行 `deriveProfile` → `ProfileRegistry.register`；有 `schedules` 的交 `AgentScheduler`。
- **FR4**: `ProfileRegistry` 新增 `remove(String name)` 和 `exists(String name)` 方法，改为可变并发 Map（已有 ConcurrentHashMap，只需补方法）。
- **FR5**: `AgentScheduler` 把 `registerAll` 循环体抽成 `registerProfile(Profile)` 方法，新增 `Map<String, ScheduledFuture<?>> scheduledTasks` 句柄表（与既有 `taskLocks` 并存）。
- **FR6**: `ContextLoader` 支持注入 Agent 正文——从 `AGENT.md` 读取时去掉 frontmatter 部分，只保留正文。正文每次现扫、不缓存，改完即时生效。
- **FR7**: `AGENT.md` 缺 `name` / `provider` 等必填字段 → 加载报错点名（`IllegalArgumentException` 或自定义异常）。
- **FR8**: `tools` 里引用底座未注册的能力 → 加载时告警（LOG.warning），不阻断注册。
- **FR9**: 示例 Agent 目录 `.ibios/agents/daily-reconcile/` 四部分俱全：`AGENT.md` + `scripts/reconcile.py` + `skills/report-format.md` + `REFERENCE.md`，按课件 §1.3-1.4 全文产出。
- **FR10**: Agent 目录派生的 Profile 与手写 Profile YAML 过同一套校验（provider 存在、tool 已注册），异常类型和消息一致。

## 4. Out of Scope (Boundary)

明确不做（边界）：
- Agent 版本管理
- Agent 市场 / 共享
- 跨 Agent 的能力复用（本版每个 Agent 独立自足）
- L3 脚本的容器 / 网络隔离（核心阶段只到解释器 + 目录白名单）
- Agent 同名冲突策略
- 文件监听热加载（第 30 节做）
- 通过 API 新增/修改/删除 Agent（第 30 节做）
- `.ibios/profiles/` 目录保留但不再新增手写 Profile（兼容前序节）

## 5. Acceptance Criteria

### Automated (Harness)

`mvn clean verify` 全绿即通过。测试类及覆盖点：

- **AgentLoaderTest**：正确拆出 `AGENT.md` 的 frontmatter 与正文；认出 `scripts/`/`skills/`/`REFERENCE.md` 资源路径；缺 `name`/`provider` 报错点名。
- **DeriveProfileTest**：frontmatter 各字段正确映射到 `Profile`；`schedules` 原样带进派生的 Profile（定时来自 Agent 的直接证据）。
- **AgentScanRegisterTest**：扫一个放了 N 个 Agent 目录的目录 → `ProfileRegistry` 出现 N 个 Agent、带 `schedules` 的都进了 `AgentScheduler`。
- **ProfileRegistryRuntimeTest**：`register()` 后立即 `get()` 可见；`remove()` 后 `get()` 返回 null；`exists()` 正确返回 true/false；非法配置报错与启动路径完全一致（同一异常类型 + 同一消息）。
- **AgentSchedulerRegisterTest**：`registerProfile` 后 `scheduledTasks` 有句柄（第 30 节注销前提）；cron/时区来自 `Profile.schedules`。
- **ProgressiveDisclosureTest**：Agent 正文进 system prompt；参考/脚本不预载，靠底座 `read_file`/`shell` 按需取（渐进式披露守点）。

### Manual

- 往 `.ibios/agents/` 放一个目录，`ibios profile list` / `GET /api/v1/profiles` 里出现这个 Agent，全程没写一行 Java
- Agent 的 `schedules` 到点真自己触发（需真模型 + 真 webhook）
- 改 Agent 目录里的正文不重启，下一次触发就用新说明

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 16**: `Profile` 值对象、`ProfileRegistry`（register/get/getAll）、`ProfileLoader`
- **Lesson 17**: `AgentService`（统一处理入口）、`ReActLoop`、`PromptBuilder`、`ContextLoader`、`ProfileContext`
- **Lesson 20**: `ToolRegistry`（校验 tool 是否已注册）、`IbiosTool`
- **Lesson 25**: `AgentScheduler`（定时注册）、`ScheduledTaskStore`
- **Lesson 26**: Web Service（`GET /api/v1/profiles` 端点验证 Agent 可见）

### External Dependencies

- SnakeYAML（已在 ibios-core 依赖中）
- Spring Scheduling（已在 ibios-core 依赖中，AgentScheduler 使用）
- 示例 Agent 的脚本用 Python 标准库，无外部依赖

---

## Clarifications

### Session 2026-10-16

- Q: Agent 正文如何进入 system prompt？ → A: Profile record 新增 `agentDir` 字段（`Path` 类型，手写 Profile 为 null）。`ContextLoader.loadAgentBody(profile)` 当 `agentDir` 非空时读 `{agentDir}/AGENT.md`、去 frontmatter 取正文。`PromptBuilder` 在 `identity.prompt` 之后、bootstrap 之前插入 agent body。

## Constitution Check

Verify this spec complies with `.specify/memory/constitution.md`:

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith
- [x] Principle 2: Five core capabilities priority
- [x] Principle 3: Self-implemented ReAct loop（不改动）
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED（不涉及）
- [x] Principle 5: Plugin tool three-tier access（Agent 用既有 Tool，不新造）
- [x] Principle 6: Synchronous + virtual threads（不涉及）
- [x] Principle 7: Sandbox interface-first（脚本走既有 shell 沙箱）
- [x] Principle 8: SQLite + audit day-one（不涉及新表）
- [x] Principle 9: Explicit provider mapping（deriveProfile 走显式 provider name）
- [x] Principle 10: Global invariants（涉外 IO 过 Sandbox、审计落表、无明文 key、session_id 只在 SessionManager 内拼接、无 Reactor/CompletableFuture、无 Spring AI 自动 tool 执行）

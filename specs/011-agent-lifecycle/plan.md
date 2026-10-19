# Implementation Plan: Agent Lifecycle Management

> Created by `/speckit-plan`. Lesson 30 — 动态管理 Agent。

## Metadata

- **Feature**: Agent Lifecycle Management
- **Spec**: `specs/011-agent-lifecycle/spec.md`
- **Date**: 2026-10-16
- **Status**: draft

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data JPA. Credentials via environment variable placeholders, no plaintext. SQLite uses manual schema scripts, not `hibernate.ddl-auto=update`.

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `AgentLifecycleService` | ibios-core (`com.ibios.core.agent`) |
| `AgentStore` | ibios-core (`com.ibios.core.agent`) |
| `WorkspaceWatcher` | ibios-core (`com.ibios.core.agent`) |
| `ToolExecutionContext` | ibios-core (`com.ibios.core.agent`) |
| `AgentView` / `FileNode` / `SessionView` | ibios-web (`com.ibios.web.model`) |
| `CreateAgentRequest` / `UpdateAgentRequest` / `SaveFilesRequest` / `GenerateRequest` | ibios-web (`com.ibios.web.model`) |
| `AgentApiController`（扩展） | ibios-web (`com.ibios.web.controller`) |
| `WorkspaceApiController` | ibios-web (`com.ibios.web.controller`) |
| `AgentStore` 实现（文件操作） | ibios-core (`com.ibios.core.agent`) |
| `IbiosAuthorProperties` | ibios-boot (`com.ibios.config`) |
| `ToolExecutionContext` 织入 `ToolExecutor` | ibios-core（改既有） |
| `MemoryServiceImpl` per-agent 路径 | ibios-memory（改既有） |

## 3. Design Decisions

### Decision 1: 两条录入路径一段注册代码

API `create()` 写完目录后调 `register(agentDir)`，`WorkspaceWatcher` 监听到变更也调同一个 `register(agentDir)`。底层都是 `AgentLoader.deriveProfile → ProfileRegistry.register → AgentScheduler.registerProfile`。这是"API 建的和手工丢目录建的，行为一模一样"的技术保证。

### Decision 2: create 失败回滚

`create` 编排：① 写 Agent 目录 → ② deriveProfile + register → ③ registerProfile。中途失败把已写目录删回（`agentStore.delete`），不留半个 Agent。

### Decision 3: delete 顺序钉死

先 `agentScheduler.unregisterProfile`（停定时）→ `profileRegistry.remove`（移出索引）→ `agentStore.archive`（归档）。顺序反了会有窗口期 cron 触发空指针。

### Decision 4: ToolExecutionContext 用 ThreadLocal

同步阻塞模型（宪法七）下一次 ReAct 循环跑在一条虚拟线程上。`ToolExecutor.execute` 前置入 `profile.name()`、执行后清除。`save_memory`/`recall_memory` 读 `ToolExecutionContext.get()` 决定落哪个 `MEMORY.md`。无 Agent 上下文时回退全局路径——向后兼容。

### Decision 5: 生成用 provider/model 走配置

新增 `ibios.author.provider` / `ibios.author.model`。provider 缺省取 `ibios.providers` 第一个；model 留空则 generate-files 端点返回 503。不发明"系统默认 model"概念。

### Decision 6: WorkspaceWatcher 用 JDK WatchService

后台守护线程（跟 `AgentScheduler` 调度线程同类，不违反宪法七）。启动全量扫 + 之后监听 `CREATE`/`DELETE`/`MODIFY` 事件。单个坏目录 catch + warn，不拖垮监听。macOS WatchService 不监听子目录内文件改动——编辑 `AGENT.md` 需显式调 update 重注册。

## 4. Test Strategy

测试策略按课件"验收 harness"执行：

- `AgentLifecycleServiceTest`：create 按序、name 冲突早拒、注册失败回滚、create 与 watcher 同一段 register、delete 顺序、update 先 unregister 后 register
- `WorkspaceWatcherTest`：手工丢目录 → register、删目录 → 注销、坏目录不崩
- `WorkspaceApiControllerTest`：tree 结构、**目录穿越 → 400**、正常文件返回
- `GenerateTest`：产出可解析、不落盘不注册、非法 → 400
- `AgentApiControllerTest`：薄转发、冲突 400、不存在 404、统一 ApiResponse

单测默认跑、集成冒烟打 `@Tag("integration")` CI 跳过；实现完成的定义是 `mvn clean verify` 全绿。

## 5. Syntax Restrictions

避开 P3C/ASM 解析不了的 Java 18+ 语法形态（如增强 switch 的 `default ->` 写法），静态检查是构建门禁。测试方法名英文，`@DisplayName` 保留中文。

## 6. Constitution Check

- [x] 原则一：自实现 ReAct loop（不依赖 Spring AI Agent）
- [x] 原则二：Spring AI 只用 Provider + Schema
- [x] 原则三：同步 + 虚拟线程（WorkspaceWatcher 是基础设施守护线程，不是请求链路异步）
- [x] 原则四：审计 Day One（generate 走 ProviderService.chat，落 llm_calls）
- [x] 原则五：LLM 调用落审计
- [x] 原则六：无 Reactor/CompletableFuture/自建线程池
- [x] 原则七：涉外 IO 首行过 Sandbox（文件浏览防穿越）
- [x] 原则八：session_id 只在 SessionManager 内拼接
- [x] 原则九：显式 Provider 映射
- [x] 原则十：无明文密钥
- [x] 模块落位遵循 §10 依赖方向（core 不依赖 Spring）

## 7. Phases

### Phase 1: 基础设施

- `AgentStore`（write / writeAll / archive / delete）
- `ToolExecutionContext`（ThreadLocal）
- `IbiosAuthorProperties`（配置绑定）
- `AgentScheduler.unregisterProfile`
- 模板文件（脚手架用）

### Phase 2: 核心编排

- `AgentLifecycleService`（create / register / get / list / update / delete / generateFiles / saveFiles）
- `WorkspaceWatcher`（启动全量扫 + WatchService 监听）
- `ToolExecutionContext` 织入 `ToolExecutor`
- per-agent memory 路径（`MemoryServiceImpl` 改造）

### Phase 3: Web 层

- `AgentApiController` 扩展（12 个端点）
- `WorkspaceApiController`（tree / file GET / file POST）
- DTO（AgentView / FileNode / SessionView / 请求体）
- 异常处理扩展

### Phase 4: 测试（harness）

- `AgentLifecycleServiceTest`
- `WorkspaceWatcherTest`
- `WorkspaceApiControllerTest`
- `GenerateTest`
- `AgentApiControllerTest`

### Phase 5: 前端

- Agent 列表页（含新建）
- Agent 详情页 5 tab（基本信息 / 生成 / 文件 / 会话 / 记忆）

### Phase 6: 配置与集成

- `application.yaml` 新增 `ibios.author.*`
- `IbiosConfiguration` 注册新 Bean
- `.ibios/archive/` 目录
- `AgentInitializer` 改造（交给 WorkspaceWatcher 统一扫描）

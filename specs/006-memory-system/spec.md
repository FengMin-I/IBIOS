# Feature Specification: Memory 三层记忆——可插拔长期记忆后端与 Agent 记忆读写工具

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: Memory 三层记忆
- **Lesson**: 22
- **Author**: IBIOS Team
- **Date**: 2026-10-15
- **Status**: draft

---

## 1. Background & Value

Agent 已经具备推理（ReAct）和工具调用（Tool）能力，但缺少跨会话的记忆能力——每次对话都是从零开始，无法记住用户的偏好、历史决策或项目背景。Memory 模块为 Agent 补上"记忆"这第三大核心能力：一个 `MemoryService` 门面统一收口会话记忆和长期记忆，长期记忆通过 `LongTermMemoryStore` 可插拔后端接口实现三档递进（Markdown 文件 → SQLite → Mem0），靠一行配置切换、上层零改动。一次实现三档是为了当场兑现"接口墙"的设计价值——同一套上层代码和测试，底层从文件系统平滑换到外部记忆框架。

## 2. User Scenarios

### Scenario 1: Agent 主动记住用户偏好

Agent 在对话中得知用户偏好用 Java 而非 Python，调用 `save_memory(content="用户偏好用 Java", scope="core")` 写入核心记忆区。下次组装 prompt 时，核心记忆完整带入 system prompt——"始终在场"，无需用户重复说明。

### Scenario 2: Agent 按关键词回忆历史记忆

Agent 需要回忆之前记录的某个项目背景，调用 `recall_memory(keyword="项目X")` 按关键词检索归档记忆区。命中结果返回给 Agent 作为上下文继续对话；未命中时返回友好提示而非抛异常。

### Scenario 3: 运维场景平滑升级记忆后端

记忆量从几十条涨到上千条，运维人员将 `memory.backend` 从 `markdown` 改为 `sqlite`。截断从字符串裁变成 SQL `LIMIT`、检索从行扫变成 `LIKE`，上层 `MemoryTools`、`PromptBuilder`、`ReActLoop` 一行代码不改。

## 3. Functional Requirements

- **FR1**: `MemoryService` 门面接口提供三个方法：`buildContext(session)` 返回核心记忆 + 会话历史供 `PromptBuilder` 拼入 system prompt；`remember(content, scope)` 写长期记忆；`recall(keyword)` 检索归档记忆。
- **FR2**: `MemoryScope` 枚举定义两值：`CORE`（核心记忆，始终在场、完整注入、不截断、不参与检索）和 `ARCHIVAL`（归档记忆，可截断、可检索）。
- **FR3**: `LongTermMemoryStore` 后端接口定义三个方法：`append(content, scope)` 按 scope 分区写入；`load()` 返回核心全量 + 归档截断后内容；`recallByKeyword(keyword)` 只在归档区做关键词检索。
- **FR4**: `MarkdownMemoryStore`（默认后端）操作 `.ibios/memory/MEMORY.md` 文件，按 `## 核心记忆` / `## 归档记忆` 两个 header 分区；截断为归档段字符串裁切（阈值 4000 字符）；检索为行包含匹配。
- **FR5**: `SqliteMemoryStore` 将记忆按条存入 `memory_entries` 表（手工建表脚本）；截断变为归档查询 `LIMIT N`（阈值 100 条）；检索变为 SQL `LIKE`；核心区 `WHERE scope='CORE'` 全量取。
- **FR6**: `Mem0MemoryStore` 接自托管 Mem0 server 的 REST API；`append` 翻译成 Mem0 add（scope 落 metadata）；`load` 按 scope 过滤取；`recall` 翻译成 Mem0 search（语义检索）。
- **FR7**: 四条行为契约所有后端必须遵守：①不缓存（每次重新读文件/查库/调 API）②核心记忆永不被截断，截断只作用在归档区 ③scope 由调用方显式指定，缺省 `archival` ④recall 是简单关键词匹配（核心阶段不做复杂分词/正则）。
- **FR8**: `MemoryTools` 提供两个内置 Tool：`save_memory(content, scope)` 和 `recall_memory(keyword)`，只依赖 `MemoryService`，对具体后端无感。通过 `@Tool` 注解自动注册到 `ToolRegistry`。
- **FR9**: 配置项 `memory.backend`（`markdown` | `sqlite` | `mem0`）决定装配哪个 `LongTermMemoryStore` 实现，换后端只改这一行配置。
- **FR10**: `PromptBuilder` 组装 system prompt 时调用 `memoryService.buildContext(session)` 把长期记忆注入，与会话历史一起提供给 ReAct 循环。

## 4. Out of Scope (Boundary)

- `save_memory` 的自动触发（核心阶段不做自动抽取，写入时机由 Agent 判断）
- Memory Wiki 式结构化矛盾检测
- 记忆压缩（超长简单截断，不做智能摘要）
- 知识图谱后端（第 21 节评审：门槛比向量更高，扩展阶段再考虑）
- 内置向量库（需要语义检索时切 Mem0 档由外部服务承担）
- 情景记忆（放扩展阶段）

## 5. Acceptance Criteria

### Automated (Harness)

| 测试类 | 覆盖的验收点 |
|--------|-------------|
| `MemoryStoreContractTest` | 参数化遍历三个 `LongTermMemoryStore` 实现，同一套断言全过：写后立读（契约一）、截断只裁归档核心区一字不动（契约二）、scope 路由到正确区块（契约三）、recall 只搜归档区（契约四） |
| `MarkdownMemoryStoreTest` | Markdown 特有：字符串截断边界、区块 header 解析 |
| `SqliteMemoryStoreTest` | SQLite 特有：手工建表脚本能建能读、归档 `LIMIT` 生效、`LIKE` 检索 |
| `Mem0MemoryStoreTest` | mock RestClient：`append` 请求体带 scope、`recall` 转发查询——不碰真 server |
| `MemoryToolsTest` | scope 缺省写归档；关键词未命中返回"没有找到相关记忆"而不抛异常 |
| `MemoryServiceImplTest` | `buildContext` 返回长期记忆（核心全量 + 归档截断后），核心记忆完整在内 |

### Manual

- 三档切换验证：`memory.backend` 分别设成 `markdown` / `sqlite`，各跑一次对话，`save_memory` 写入、下一轮 `buildContext` 带上——同一段对话、同一个体感
- Mem0 档真连一次（可选）：部署自托管 Mem0，验证 `save_memory` 进了 Mem0、`recall` 能语义召回
- 用真模型完整走一遍：Agent 主动调 `save_memory`，开新会话系统提示带核心记忆
- `MEMORY.md` 和 `USER.md` 角色分清：`USER.md` 只读、`MEMORY.md` 可被 Agent 写入

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 17**: `ReActLoop`、`PromptBuilder`、`AgentService`、`ProfileContext`、`ContextLoader`（ibios-core）——`MemoryService` 集成到 `PromptBuilder` 的 prompt 组装流程
- **Lesson 18**: `SessionManager` 接口（ibios-core）、`Session` 实体 + `SessionRepository`（ibios-storage）——`buildContext` 需要获取会话历史
- **Lesson 20**: `IbiosTool` 接口、`ToolRegistry`、`ToolResult`（ibios-core）——`MemoryTools` 通过 `IbiosTool` 抽象注册

### External Dependencies

- Mem0 server（自托管部署，契约测试用内存替身不碰真 server）
- 复用项目已有 SQLite（不引入新数据库依赖）
- `memory.backend` 配置项缺省值 `markdown`

### Assumptions

- `MEMORY.md` 文件不存在时自动创建，初始为空
- Mem0 档的 REST API 格式以实际部署的 Mem0 版本为准，契约测试用内存替身覆盖行为契约
- SQLite 后端的 `memory_entries` 表通过手工建表脚本创建（Flyway），不依赖 `ddl-auto`
- Mem0 凭证和地址走环境变量 `${MEM0_BASE_URL}`，不落明文

---

## Constitution Check

- [x] Principle 1: JDK 21 + Spring Boot 3.x 单体——Memory 模块在 ibios-memory 内，不引入新运行时
- [x] Principle 2: 五大核心能力优先——Memory 是核心能力三，属于核心阶段交付
- [x] Principle 3: 自实现 ReAct 循环——MemoryService 被 PromptBuilder 调用，不引入外部 Agent 框架
- [x] Principle 4: Spring AI 只用协议——MemoryTools 用 @Tool 注解生成 Schema，不用 Spring AI 执行管线
- [x] Principle 5: Plugin Tool 三档访问——MemoryTools 作为内置 Tool 通过 IbiosTool 注册
- [x] Principle 6: 同步 + 虚拟线程——Memory 读写全部同步，无 Reactor/CompletableFuture
- [x] Principle 7: Sandbox 接口先行——Mem0MemoryStore 的 HTTP 调用需过 Sandbox.enforce（24 节接线，当前留调用位）
- [x] Principle 8: SQLite + 审计 Day One——SqliteMemoryStore 用手工建表脚本，Flyway 管理
- [x] Principle 9: 显式 Provider 映射——不涉及 Provider 变更
- [x] Principle 10: 全局不变量——①Mem0 HTTP 调用过 Sandbox（留位）②不涉及 LLM/Tool 审计变更 ③无明文 key（Mem0 走环境变量）④session_id 只在 SessionManager 内 ⑤无 Reactor ⑥无 Spring AI 自动执行

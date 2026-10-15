# Implementation Plan: Memory 三层记忆

> Created by `/speckit-plan`. Fill all sections before proceeding to tasks.

## Metadata

- **Feature**: Memory 三层记忆（Lesson 22）
- **Spec**: `specs/006-memory-system/spec.md`
- **Date**: 2026-10-15
- **Status**: draft

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists
in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data
JPA. Credentials via environment variable placeholders, no plaintext. SQLite
uses manual schema scripts, not `hibernate.ddl-auto=update`.

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `MemoryService`（接口） | ibios-core |
| `MemoryScope`（枚举） | ibios-core |
| `LongTermMemoryStore`（接口） | ibios-core |
| `MemoryServiceImpl`（实现） | ibios-memory |
| `MarkdownMemoryStore` | ibios-memory |
| `SqliteMemoryStore` | ibios-memory |
| `Mem0MemoryStore` | ibios-memory |
| `MemoryEntry`（JPA 实体） | ibios-memory |
| `MemoryEntryRepository` | ibios-memory |
| `MemoryTools`（save_memory / recall_memory） | ibios-memory |
| `MemoryConfiguration`（装配） | ibios-memory |
| `memory_entries` 建表 SQL | ibios-boot（schema.sql） |
| `memory.backend` 配置项 | ibios-boot（application.yaml） |

**接口与实现分离的理由**：`MemoryService`、`MemoryScope`、`LongTermMemoryStore` 三个接口/枚举放 ibios-core，因为 `PromptBuilder`（ibios-core）需要引用 `MemoryService` 来组装 prompt。实现类全在 ibios-memory，依赖方向 ibios-memory → ibios-core 单向。

## 3. Design Decisions

### Decision 1: 接口墙——MemoryService 与 LongTermMemoryStore 双层解耦

上层（PromptBuilder / MemoryTools）只认 `MemoryService`，`MemoryService` 只认 `LongTermMemoryStore`。换后端时 `MemoryService` 以上零改动。这是第 21 节评审的核心结论，本节的代码就是兑现。

** alternatives considered**: 单层（MemoryService 直接包含三档逻辑）——耦合高、加新后端要改门面类，否决。

### Decision 2: 契约测试参数化——同一套断言跑三个实现

`MemoryStoreContractTest` 用 `@ParameterizedTest` + `@MethodSource("allStores")` 对三个 `LongTermMemoryStore` 实现统一跑四条契约。Mem0 档在契约测试中用内存替身（`InMemoryMem0Stub`）替掉 REST 调用，真实 REST 交互由 `Mem0MemoryStoreTest` 单独 mock 验证。

### Decision 3: memory_entries 表走 Flyway 手工建表

与 sessions / tool_invocations / llm_calls 同口径，在 `schema.sql` 中追加 `memory_entries` 建表语句。不依赖 `ddl-auto=update`（宪法八）。

### Decision 4: Mem0MemoryStore 的 HTTP 调用留 Sandbox 调用位

Mem0 的 REST 调用属于涉外 IO，首行应过 `Sandbox.enforce`。但 Sandbox 接口在第 24 节才实现，当前在 `Mem0MemoryStore` 中留注释调用位注明"24 节接线"（宪法十·全局不变量一）。

### Decision 5: MemoryTools 通过 @Tool 注解 + IbiosTool 包装注册

`MemoryTools` 类中 `saveMemory` 和 `recallMemory` 方法标 `@Tool` 注解，由 Spring AI 生成 JSON Schema。包装成 `IbiosTool` 实例注册到 `ToolRegistry`，与其他内置 Tool 一视同仁（宪法五）。

## 4. Test Strategy

Tests follow the lesson's acceptance harness:

- **`MemoryStoreContractTest`**：参数化遍历三个 `LongTermMemoryStore` 实现——写后立读（契约一）、截断只裁归档核心区一字不动（契约二）、scope 路由到正确区块（契约三）、recall 只搜归档区（契约四）
- **`MarkdownMemoryStoreTest`**：字符串截断边界、区块 header 解析
- **`SqliteMemoryStoreTest`**：手工建表脚本能建能读、归档 `LIMIT` 生效、`LIKE` 检索
- **`Mem0MemoryStoreTest`**：mock RestClient，append 请求体带 scope、recall 转发查询
- **`MemoryToolsTest`**：scope 缺省写归档；关键词未命中返回"没有找到相关记忆"不抛异常
- **`MemoryServiceImplTest`**：`buildContext` 返回长期记忆（核心全量 + 归档截断后），核心记忆完整在内

Unit tests run by default; integration smoke tests tagged `@Tag("integration")` skipped in CI. Implementation is done when `mvn clean verify` is all green.

## 5. Syntax Restrictions

Avoid Java 18+ syntax that P3C/ASM cannot parse (e.g., enhanced switch
`default ->` form). Static analysis is a build gate.

## 6. Constitution Check

- [x] Principle 1: ibios-memory 模块已在 Maven 多模块结构中，不引入新运行时
- [x] Principle 2: Memory 是核心能力三，属于核心阶段交付
- [x] Principle 3: MemoryService 被 PromptBuilder 调用，不引入外部 Agent 框架
- [x] Principle 4: MemoryTools 用 @Tool 注解生成 Schema，不用 Spring AI 执行管线
- [x] Principle 5: MemoryTools 作为内置 Tool 通过 IbiosTool 注册
- [x] Principle 6: Memory 读写全部同步，无 Reactor/CompletableFuture
- [x] Principle 7: Mem0 HTTP 调用留 Sandbox.enforce 调用位（24 节接线）
- [x] Principle 8: memory_entries 走手工建表脚本，Flyway 管理
- [x] Principle 9: 不涉及 Provider 变更
- [x] Principle 10: ①Mem0 HTTP 过 Sandbox（留位）②不涉及 LLM/Tool 审计变更 ③Mem0 凭证走环境变量 ④session_id 只在 SessionManager ⑤无 Reactor ⑥无 Spring AI 自动执行

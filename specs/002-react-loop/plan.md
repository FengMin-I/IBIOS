# Implementation Plan: ReAct 循环引擎

> Created by `/speckit-plan`. Fill all sections before proceeding to tasks.

## Metadata

- **Feature**: ReAct 循环引擎
- **Spec**: `specs/002-react-loop/spec.md`
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data JPA. Credentials via environment variable placeholders, no plaintext. SQLite uses manual schema scripts, not `hibernate.ddl-auto=update`.

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `ReActLoop` | ibios-core |
| `PromptBuilder` | ibios-core |
| `ToolExecutor` | ibios-core |
| `AgentService` | ibios-core |
| `ProfileContext` | ibios-core |
| `ContextLoader` | ibios-core |
| `ToolInvocation` (JPA entity) | ibios-storage |
| `ToolInvocationRepository` | ibios-storage |
| `JpaToolInvocationAudit` | ibios-storage |
| `ToolInvocationAudit` (interface) | ibios-storage |
| `tool_invocations` 建表脚本 | ibios-boot (schema.sql) |

## 3. Design Decisions

### Decision 1: ReActLoop 自实现，不依赖 Spring AI Agent 抽象

**描述**: ReActLoop 是 Agent 的核心循环引擎，完全自实现。Spring AI 只用于 Provider 抽象和协议转换，禁用其自动工具执行。

**替代方案**: 使用 Spring AI 的 `AgentExecutor` 或 `ChatClient` 的 tool-calling pipeline。

**理由**: 循环是 Agent 最需要自己掌控的地方——什么时候停、工具失败了怎么办、上下文太长了怎么压、哪几步想换个模型，这些都得能自己调。用框架的黑盒，这些就动不了。宪法原则三明确要求自实现。

### Decision 2: ProfileContext 用 ThreadLocal 传递当前 Profile

**描述**: AgentService 在处理入口处把 Profile 放进 ThreadLocal，出口处 finally 清掉。工具执行时通过 `ProfileContext.current()` 读取当前 Agent 配置。

**替代方案**: 修改 `IbiosTool.execute` 签名增加 Profile 参数；用 Spring RequestScope Bean。

**理由**: 改工具接口签名代价太大（所有工具都要改），ThreadLocal 在虚拟线程下每请求独占线程天然不串。宪法原则六（同步+虚拟线程）支持此方案。必须在 finally 中清除，防止线程复用泄漏。

### Decision 3: tool_invocations 表结构与 llm_calls 口径一致

**描述**: tool_invocations 表包含 session_id、tool_name、input_json、output_json、success、error_message、duration_ms、created_at 等字段，与 llm_calls 表保持相同的审计口径。

**替代方案**: 只记成功不记失败；用不同字段命名。

**理由**: 宪法原则八要求审计 day-one，成功和失败都要记录。与 llm_calls 口径一致便于后续统一查询和报表。

### Decision 4: ContextLoader 每次重新读文件，不缓存

**描述**: 每次组装 prompt 时，ContextLoader 都重新从文件系统读取 Bootstrap 和 Skill 文件内容。

**替代方案**: 启动时加载一次并缓存；文件变更监听 + 增量更新。

**理由**: 课件明确要求"每次组装 prompt 都重新读文件、不缓存"，确保用户改完立即生效。性能开销可接受（文件小、读取快）。Profile 显式引用的文件缺失报错，Bootstrap 缺失至少 WARN。

## 4. Test Strategy

Tests follow the lesson's acceptance harness: ReActLoopTest, PromptBuilderTest, ToolExecutorTest, AgentServiceTest, ContextLoaderTest covering 无工具调用一轮收尾、有工具调用执行并回填、转满最大轮数强制停、每轮响应和工具结果累积进 Session、四部分顺序正确、历史超 N 轮被截断、system prompt 末尾含当前日期时间、成功写审计 success=true、失败也写 success=false 带原因异常不吞、处理期间 ProfileContext 可取到当前 Profile、处理抛异常时 finally 也清掉、结束后 Session 被持久化、改文件后下一次 build 立即读到新内容、Skill 引用缺失报错、Bootstrap 缺失 WARN. Unit tests run by default; integration smoke tests tagged `@Tag("integration")` skipped in CI. Implementation is done when `mvn clean verify` is all green.

## 5. Syntax Restrictions

Avoid Java 18+ syntax that P3C/ASM cannot parse (e.g., enhanced switch `default ->` form). Static analysis is a build gate.

## 6. Constitution Check

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith — 同步执行模型，无第二运行时
- [x] Principle 2: Five core capabilities priority — ReAct 循环是五大能力之二的核心实现
- [x] Principle 3: Self-implemented ReAct loop — ReActLoop/PromptBuilder/ToolExecutor 全部自实现
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED — ToolExecutor 是唯一工具执行路径
- [x] Principle 5: Plugin tool three-tier access — ToolExecutor 通过 ToolRegistry 统一调度
- [x] Principle 6: Synchronous + virtual threads — 同步阻塞模型，ProfileContext 用 ThreadLocal
- [x] Principle 7: Sandbox interface-first — ToolExecutor 执行前过沙箱检查（留调用位）
- [x] Principle 8: SQLite + audit day-one — tool_invocations 表 day one 写入
- [x] Principle 9: Explicit provider mapping — 通过 ProviderService 调用
- [x] Principle 10: Global invariants — 涉外 IO 首行过 Sandbox（留位）、审计完整、无明文 key、无 Reactor、无 Spring AI 自动工具执行

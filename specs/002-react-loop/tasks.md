# Tasks: ReAct 循环引擎

> Created by `/speckit-tasks`. Dependency-ordered, each task testable.

## Metadata

- **Feature**: ReAct 循环引擎
- **Plan**: `specs/002-react-loop/plan.md`
- **Date**: 2026-10-14
- **Status**: pending

---

## Task Categories

- **[C]** — Configuration / Schema
- **[T]** — Test (harness)
- **[I]** — Implementation
- **[V]** — Verification

## Task List

### Phase 1: Setup — tool_invocations 审计表

**目标**: 创建 tool_invocations 表的建表脚本和 JPA 实体，为工具调用审计做准备。

- [ ] T001 [C] 在 `ibios-boot/src/main/resources/schema.sql` 中添加 `tool_invocations` 建表语句（含 id, session_id, tool_name, input_json, output_json, success, error_message, duration_ms, created_at 列）
  - **Depends on**: —
  - **Deliverables**: `ibios-boot/src/main/resources/schema.sql`（追加）
  - **Acceptance**: 应用启动时表创建成功，字段与 llm_calls 口径一致

### Phase 2: Foundational — 审计实体 + ProfileContext + ContextLoader

**目标**: 实现 ToolInvocation 审计实体和 Repository、ProfileContext ThreadLocal、ContextLoader 文件读取。这些是 ReActLoop 的前置依赖。

- [ ] T002 [I] 在 `ibios-storage/src/main/java/com/ibios/storage/audit/ToolInvocation.java` 创建 JPA 实体
  - **Depends on**: T001
  - **Deliverables**: `ToolInvocation.java`
  - **Acceptance**: 实体字段与 schema.sql 一致，包含 success/error_message 列映射

- [ ] T003 [I] 在 `ibios-storage/src/main/java/com/ibios/storage/audit/ToolInvocationRepository.java` 创建 Spring Data JPA Repository
  - **Depends on**: T002
  - **Deliverables**: `ToolInvocationRepository.java`
  - **Acceptance**: 继承 JpaRepository<ToolInvocation, Long>，可注入使用

- [ ] T004 [I] 在 `ibios-storage/src/main/java/com/ibios/storage/audit/ToolInvocationAudit.java` 创建审计接口
  - **Depends on**: T002
  - **Deliverables**: `ToolInvocationAudit.java`（接口）
  - **Acceptance**: 定义 recordToolInvocation(ToolInvocation) 方法

- [ ] T005 [I] 在 `ibios-storage/src/main/java/com/ibios/storage/audit/JpaToolInvocationAudit.java` 创建审计接口实现
  - **Depends on**: T003, T004
  - **Deliverables**: `JpaToolInvocationAudit.java`
  - **Acceptance**: 实现 ToolInvocationAudit 接口，调用 repository.save()

- [ ] T006 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/ProfileContext.java` 创建 ThreadLocal 包装类
  - **Depends on**: —
  - **Deliverables**: `ProfileContext.java`
  - **Acceptance**: 提供 static set(Profile)/current()/clear() 方法，内部用 ThreadLocal<Profile>

- [ ] T007 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/ContextLoader.java` 创建上下文加载器
  - **Depends on**: —
  - **Deliverables**: `ContextLoader.java`
  - **Acceptance**: 按 Profile 的 bootstrap 和 skills 字段读取文件，每次重新读不缓存；Bootstrap 缺失 WARN，Skill 缺失报错

### Phase 3: US-1 Core — ReActLoop + PromptBuilder + ToolExecutor

**目标**: 实现 ReAct 循环的三个核心组件，并通过对应的 harness 测试。

**独立测试标准**: ReActLoopTest/PromptBuilderTest/ToolExecutorTest 全部通过，覆盖：无工具调用一轮收尾、有工具调用执行并回填、转满最大轮数强制停、每轮响应和工具结果累积进 Session、四部分顺序正确、历史超 N 轮被截断、system prompt 末尾含当前日期时间、成功写审计 success=true、失败也写 success=false 带原因异常不吞。

- [ ] T008 [T] 在 `ibios-core/src/test/java/com/ibios/core/react/PromptBuilderTest.java` 创建测试类
  - **Depends on**: T006, T007
  - **Deliverables**: `PromptBuilderTest.java`
  - **Acceptance**: 包含三个测试方法：四部分顺序正确、历史超 N 轮被截断、system prompt 末尾含当前日期时间

- [ ] T009 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/PromptBuilder.java` 实现 Prompt 组装器
  - **Depends on**: T007
  - **Deliverables**: `PromptBuilder.java`
  - **Acceptance**: build(Session, Profile) 方法按顺序拼接四部分：system prompt（AGENT.md + Bootstrap + 当前日期时间）+ Memory 注入 + 对话历史（按 maxHistoryTurns 截断，默认 20）+ 工具列表；T008 测试通过

- [ ] T010 [T] 在 `ibios-core/src/test/java/com/ibios/core/react/ToolExecutorTest.java` 创建测试类
  - **Depends on**: T005
  - **Deliverables**: `ToolExecutorTest.java`
  - **Acceptance**: 包含两个测试方法：成功写审计 success=true、失败也写 success=false 带原因异常不吞

- [ ] T011 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/ToolExecutor.java` 实现工具执行器
  - **Depends on**: T005
  - **Deliverables**: `ToolExecutor.java`
  - **Acceptance**: execute(sessionId, ToolCall) 方法从 ToolRegistry 找工具、执行沙箱检查（留调用位注明第 24 节接线）、执行工具、返回 ToolResult，同时写 tool_invocations 审计表；T010 测试通过

- [ ] T012 [T] 在 `ibios-core/src/test/java/com/ibios/core/react/ReActLoopTest.java` 创建测试类
  - **Depends on**: T009, T011
  - **Deliverables**: `ReActLoopTest.java`
  - **Acceptance**: 包含四个测试方法：无工具调用一轮收尾、有工具调用执行并回填、转满最大轮数强制停、每轮响应和工具结果累积进 Session

- [ ] T013 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/ReActLoop.java` 实现 ReAct 循环引擎
  - **Depends on**: T009, T011
  - **Deliverables**: `ReActLoop.java`
  - **Acceptance**: run(Session, String, Profile) 方法实现循环：组装 Prompt → 调 ProviderService.chat → 累积到 Session → 无工具调用则返回，有则交给 ToolExecutor 执行并回填 → 进入下一轮；最大迭代次数从 Profile.maxIterations 读取（默认 10），达到后返回"达到最大轮数，已停止"；T012 测试通过

### Phase 4: US-1 Orchestration — AgentService

**目标**: 实现 AgentService 编排层，统一三种触发源的入口，并通过对应的 harness 测试。

**独立测试标准**: AgentServiceTest 全部通过，覆盖：处理期间 ProfileContext 可取到当前 Profile、处理抛异常时 finally 也清掉、结束后 Session 被持久化。

- [ ] T014 [T] 在 `ibios-core/src/test/java/com/ibios/core/react/AgentServiceTest.java` 创建测试类
  - **Depends on**: T013
  - **Deliverables**: `AgentServiceTest.java`
  - **Acceptance**: 包含三个测试方法：处理期间 ProfileContext 可取到当前 Profile、处理抛异常时 finally 也清掉、结束后 Session 被持久化

- [ ] T015 [I] 在 `ibios-core/src/main/java/com/ibios/core/react/AgentService.java` 实现 Agent 服务编排层
  - **Depends on**: T013, T014
  - **Deliverables**: `AgentService.java`
  - **Acceptance**: process(Session, String) 方法：设 ProfileContext → 调 ReActLoop.run → 持久化 Session → finally 清 ProfileContext；T014 测试通过

### Phase 5: Polish & Cross-Cutting Concerns

**目标**: 完成跨模块集成、上下文加载测试、全量验证。

- [ ] T016 [T] 在 `ibios-core/src/test/java/com/ibios/core/react/ContextLoaderTest.java` 创建测试类
  - **Depends on**: T007
  - **Deliverables**: `ContextLoaderTest.java`
  - **Acceptance**: 包含三个测试方法：改文件后下一次 build 立即读到新内容、Skill 引用缺失报错、Bootstrap 缺失 WARN

- [ ] T017 [V] 运行 `mvn clean verify` 验证全量构建
  - **Depends on**: T001-T016
  - **Deliverables**: 构建日志
  - **Acceptance**: 所有测试通过（含 P3C/SpotBugs/FindSecBugs/PMD），无编译错误

- [ ] T018 [V] 验证模块依赖方向
  - **Depends on**: T017
  - **Deliverables**: grep 输出
  - **Acceptance**: `ibios-core` 不依赖 Spring 框架（只有 SnakeYAML）；`ibios-storage` 依赖 `ibios-core`；无循环依赖

- [ ] T019 [V] 验证 H4 六条全局不变量
  - **Depends on**: T017
  - **Deliverables**: grep 输出
  - **Acceptance**: ①涉外 IO 首行过 Sandbox（留调用位）②LLM 调用成败都落 llm_calls、工具执行成败都落 tool_invocations ③grep 无明文 key ④session_id 只在 SessionManager 内拼接（本节不涉及）⑤无 Reactor/CompletableFuture/自建线程池 ⑥无 Spring AI 自动工具执行路径

---

## Deliverables Checklist

Cross-reference with lesson courseware's "本节交付物" section:

- [ ] **Code**: ReActLoop, PromptBuilder, ToolExecutor, AgentService, ProfileContext, ContextLoader, ToolInvocation 实体 + ToolInvocationRepository
- [ ] **Tests**: ReActLoopTest, PromptBuilderTest, ToolExecutorTest, AgentServiceTest, ContextLoaderTest
- [ ] **Config**: tool_invocations 建表脚本（schema.sql 追加）
- [ ] **Schema**: tool_invocations 表（含 success/error_message 列）
- [ ] **约定**: 最大轮数默认 10；历史截断默认 20 轮；prompt 末尾附当前日期时间

---

## Dependencies Graph

```
T001 (schema)
  ↓
T002 (ToolInvocation entity)
  ↓
T003 (Repository) + T004 (Audit interface) [parallel]
  ↓
T005 (JpaToolInvocationAudit)
  ↓
T010 (ToolExecutorTest) + T011 (ToolExecutor) [parallel]
  ↓
T006 (ProfileContext) + T007 (ContextLoader) [parallel, independent]
  ↓
T008 (PromptBuilderTest) + T009 (PromptBuilder) [parallel]
  ↓
T012 (ReActLoopTest) + T013 (ReActLoop) [parallel]
  ↓
T014 (AgentServiceTest) + T015 (AgentService) [parallel]
  ↓
T016 (ContextLoaderTest)
  ↓
T017 (mvn clean verify)
  ↓
T018 (dependency direction) + T019 (H4 invariants) [parallel]
```

## Parallel Execution Examples

**Example 1**: T003 和 T004 可以并行（不同文件，无依赖）
**Example 2**: T006 (ProfileContext) 和 T007 (ContextLoader) 可以并行
**Example 3**: T010 (ToolExecutorTest) 和 T011 (ToolExecutor) 可以并行（测试先行）
**Example 4**: T018 和 T019 可以并行（都是验证任务）

## Implementation Strategy

**MVP scope**: Phase 1-4 是核心实现，Phase 5 是验证。

**Incremental delivery**:
1. 先完成审计基础设施（T001-T005），为工具执行审计做准备
2. 实现 ProfileContext 和 ContextLoader（T006-T007），为 PromptBuilder 做准备
3. 实现 ReActLoop 三件套（T008-T013），每实现一个组件就跑对应测试
4. 实现 AgentService（T014-T015），完成编排层
5. 最后跑全量验证（T016-T019），确保所有 constitution 原则满足

**Risk mitigation**:
- 每实现一个组件就写对应测试，红了当场修，不攒到最后
- ProfileContext 必须在 finally 中清除，测试显式验证异常场景
- ContextLoader 每次重新读文件，测试验证无缓存
- ToolExecutor 成功失败都写审计，测试覆盖两种路径

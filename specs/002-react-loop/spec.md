# Feature Specification: ReAct 循环引擎

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: ReAct 循环引擎
- **Lesson**: 17
- **Author**: IBIOS Team
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. Background & Value

单独调一次大模型只是个 chatbot——用户问一句、模型答一句，完事。但很多真实任务一句话答不了，比如"查天气再给穿搭建议"需要多步推理。ReAct（Reasoning + Acting）把"想—做—看"串成一个循环：模型想下一步该干嘛、调个工具去做、拿到结果看一眼，不够就再来一轮，够了就给最终答复。这是 IBIOS 五大核心能力的第二块，也是整个 Agent 最关键的一段代码——它让 IBIOS 从 chatbot 升级为能真正做事的 Agent OS。

## 2. User Scenarios

### Scenario 1: 查天气给穿搭建议

用户问"今天天气怎么样，我该穿什么"。Agent 先调 http_get 工具查天气 API，拿到温度/天气数据后，在下一轮根据结果给出穿搭建议。整个过程在 ReAct 循环内自动完成，用户只看到最终答复。

### Scenario 2: 多步推理问题

用户问一个需要多步推理的问题（比如"对比三家公司的股价走势"）。Agent 在循环中反复调用工具（查股价、查新闻、做对比），观察每步结果，最终给出完整答复。循环自动判断何时该停——当模型不再要求调工具时，说明它能给最终答复了。

### Scenario 3: 工具调用失败降级

Agent 调用工具时遇到网络错误或 API 限流。ToolExecutor 把失败信息（含错误原因）写入 tool_invocations 审计表并返回给循环，Agent 在下一轮收到错误后可以选择重试或给出降级答复。失败全程可审计。

## 3. Functional Requirements

Each requirement MUST be testable.

- **FR1**: ReActLoop 接收 Session + 用户消息 + Profile，输出最终响应文本。循环内依次执行：组装 Prompt → 调 Provider 获取响应 → 累积到 Session → 无工具调用则返回最终响应，有则交给 ToolExecutor 执行并回填结果 → 进入下一轮。
- **FR2**: PromptBuilder 每轮组装四段式 Prompt，按顺序拼接：(1) system prompt（AGENT.md 正文 + Bootstrap 文件内容，末尾附当前日期时间）(2) Memory 注入（长期记忆，未开启则跳过）(3) 对话历史（按 maxHistoryTurns 截断，默认保留最近 20 轮）(4) 当前 Profile 可用的工具列表（Function Calling 格式）。
- **FR3**: ToolExecutor 从 ToolRegistry 找到目标工具，执行沙箱检查（Sandbox 未就位时留调用位注明第 24 节接线），执行工具，返回 ToolResult，同时写入 tool_invocations 审计表——成功记 success=true，失败记 success=false 并带 error_message，异常不吞。
- **FR4**: AgentService.process 是三种触发源（CLI/Web Service/定时任务）的统一入口。执行流程：设 ProfileContext（ThreadLocal，存当前 Profile）→ 调 ReActLoop.run 跑完循环 → 持久化 Session → finally 块清 ProfileContext。
- **FR5**: ProfileContext 是 ThreadLocal，存储当前 Profile 供工具执行时读取当前 Agent 配置。虚拟线程下每个请求独占线程，天然不串；处理结束后必须清除，防止线程复用时泄漏。
- **FR6**: ContextLoader 按 Profile 的 bootstrap 和 skills 字段读取文件系统内容。每次组装 prompt 时重新读文件（不缓存），确保用户修改后立即生效。Profile 显式引用的文件缺失时报错，Bootstrap 文件缺失至少 WARN（不静默跳过）。
- **FR7**: 最大迭代次数默认 10（可在 Profile 中覆盖），达到后强制停止循环并返回"达到最大轮数，已停止"提示，防止模型反复要求调工具导致死循环。
- **FR8**: tool_invocations 表含 success 和 error_message 列，使用手工建表脚本（不依赖 hibernate.ddl-auto=update），审计口径与 llm_calls 一致。

## 4. Out of Scope (Boundary)

Explicitly list what this feature does NOT do.

- 工具并行调用——多个 Tool 调用按顺序逐个执行，不并行
- Agent 之间互相委托——一个 Agent 不能把任务转给另一个 Agent
- 流式输出——响应是一次性返回，不是 SSE 流式
- 上下文压缩——只用"保留最近 N 轮"的简单截断策略，不做摘要压缩（扩展阶段再做）

## 5. Acceptance Criteria

### Automated (Harness)

测试策略按课件"验收 harness"执行：ReActLoopTest、PromptBuilderTest、ToolExecutorTest、AgentServiceTest、ContextLoaderTest（覆盖：无工具调用一轮收尾、有工具调用执行并回填、转满最大轮数强制停、每轮响应和工具结果累积进 Session、四部分顺序正确、历史超 N 轮被截断、system prompt 末尾含当前日期时间、成功写审计 success=true、失败也写 success=false 带原因异常不吞、处理期间 ProfileContext 可取到当前 Profile、处理抛异常时 finally 也清掉、结束后 Session 被持久化、改文件后下一次 build 立即读到新内容、Skill 引用缺失报错、Bootstrap 缺失 WARN），单测默认跑、集成冒烟打 @Tag("integration") CI 跳过；实现完成的定义是 mvn clean verify 全绿。

关键回归点：
- **死循环兜底**：模型一直要调工具时，转满最大轮数（默认 10）强制停止，一轮不多
- **ThreadLocal 泄漏**：处理中抛异常时，finally 块也必须清除 ProfileContext，防止下一个复用此线程的请求拿到别人的 Profile
- **上下文截断**：对话历史超过 maxHistoryTurns 时，只保留最近 N 轮
- **失败审计**：工具调用失败也写 tool_invocations 表，success=false 带 error_message

### Manual

- Demo 一（每日天气）的对话版用真模型跑通：问天气 → Agent 调 http_get → 拿到数据 → 给出穿搭建议，多轮对话完整走下来
- 循环是自实现的 ReActLoop，未使用 Spring AI 的 Agent/AgentExecutor 抽象（code review 确认）
- 其余验收点（死循环兜底、累积、截断、失败审计）已由 harness 覆盖，mvn test 绿即打勾

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 16 (US-1 Agent Provider)**: Profile/ProfileLoader/ProfileRegistry（ibios-core）、ProviderService + ToolSchemaAdapter（ibios-provider）、LlmCall + LlmCallRepository（ibios-storage）、IbiosTool 接口（ibios-core）

### External Dependencies

- Spring AI Alibaba（BOM 已锁定，提供 Provider 抽象和协议转换）
- SQLite + Spring Data JPA（持久化）

---

## Constitution Check

Verify this spec complies with `.specify/memory/constitution.md`:

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith — 同步执行模型，无第二运行时
- [x] Principle 2: Five core capabilities priority — ReAct 循环是五大能力之二的核心实现
- [x] Principle 3: Self-implemented ReAct loop — ReActLoop/PromptBuilder/ToolExecutor 全部自实现，不依赖 Spring AI Agent 抽象
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED — ToolExecutor 是唯一工具执行路径，Spring AI 自动执行已禁用
- [x] Principle 5: Plugin tool three-tier access — ToolExecutor 通过 ToolRegistry 统一调度，不感知工具来源
- [x] Principle 6: Synchronous + virtual threads — 同步阻塞模型，ProfileContext 用 ThreadLocal 适配虚拟线程
- [x] Principle 7: Sandbox interface-first — ToolExecutor 执行前过沙箱检查，Sandbox 未就位时留调用位
- [x] Principle 8: SQLite + audit day-one — tool_invocations 表 day one 写入，成功失败都记
- [x] Principle 9: Explicit provider mapping — 通过 ProviderService 调用，显式映射已在 Lesson 16 实现
- [x] Principle 10: Global invariants — 涉外 IO 首行过 Sandbox（留位）、审计完整、无明文 key、session_id 只在 SessionManager 拼接、无 Reactor、无 Spring AI 自动工具执行

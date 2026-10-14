# Feature Specification: 5 个扩展内置工具

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: 5 个扩展内置工具——对齐业界主流 Agent 产品的基础工具面
- **Lesson**: 20（补全）
- **Author**: IBIOS Dev Team
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. Background & Value

Lesson 20 已实现 7 个基础工具（read_file/write_file/list_dir/shell/http_get/http_post/notify），但对照 Claude Code、Cursor、OpenAI Assistants 等主流 Agent 产品，还缺 5 个日常高频工具：edit_file（局部编辑）、grep（内容搜索）、glob（文件查找）、ask_user（人机交互）、web_search（网页搜索）。补全后 IBIOS 基础工具面与业界对齐，且验证了"先立统一抽象，加工具成本恒定"的设计价值。

---

## 2. User Scenarios

### Scenario 1: Agent 局部编辑配置文件

Agent 需要修改配置文件某一行时，用 edit_file 只改那一行而非重写整个文件，省 token 且安全。edit_file 要求旧文本唯一匹配，找不到或出现多次都报错，报错时文件不动。

### Scenario 2: Agent 查找配置引用

Agent 需要查找"哪个文件引用了某个配置"时，用 grep 一步到位而非层层 list_dir + read_file。grep 按正则搜索文件内容，返回"文件:行号:内容"格式，上限 200 条，二进制/非 UTF-8 文件跳过。

### Scenario 3: Agent 向用户确认危险操作

Agent 执行危险操作前，用 ask_user 向用户确认，实现 human-in-the-loop。ask_user 依赖 UserInteraction 接口，CLI 场景读终端，定时任务/Web 场景报不支持。

### Scenario 4: 日报 Agent 上网查资料

日报/调研类 Agent 用 web_search 上网查资料，不依赖业务方各自配 MCP。web_search 依赖 SearchProvider 接口，核心阶段实现 DuckDuckGoSearchProvider（免 key 的 Instant Answer API）。

---

## 3. Functional Requirements

Each requirement MUST be testable.

- **FR1**: edit_file 在 FileTools 中新增 EditFileTool，要求旧文本唯一匹配（找不到或出现多次都报错），报错时文件不动。
- **FR2**: grep 在 FileTools 中新增 GrepTool，按正则搜索文件内容，返回"文件:行号:内容"格式，上限 200 条，二进制/非 UTF-8 文件跳过。
- **FR3**: glob 在 FileTools 中新增 GlobTool，按通配符（如 `**/*.yaml`）查找文件路径，上限 200 条。
- **FR4**: ask_user 新增 InteractionTools 模块 + UserInteraction 接口（ask 方法），两档实现：ConsoleUserInteraction（读终端）和 UnsupportedUserInteraction（无人值守渠道报不支持）。
- **FR5**: web_search 新增 WebSearchTools 模块 + SearchProvider 接口（search 方法返回 SearchResult 列表），一档实现：DuckDuckGoSearchProvider（免 key 的 Instant Answer API）。
- **FR6**: 所有新工具第一行过 Sandbox.enforce（edit_file/grep/glob 走 FILE_READ/FILE_WRITE，web_search 走 HTTP_REQUEST）。
- **FR7**: 新工具通过 IbiosTool 抽象注册到 ToolRegistry，自动纳入契约测试、审计、Profile 过滤。

---

## 4. Out of Scope (Boundary)

Explicitly list what this feature does NOT do.

- Tool Policy（allow/deny 规则）
- 工具按需加载
- 把 IBIOS 变成 MCP server
- 完整容器级沙箱（24 节交付）
- 工具并行调用

---

## 5. Acceptance Criteria

### Automated (Harness)

Tests that `mvn test` / `mvn clean verify` will run. List test classes and what they verify.

- **FileToolsTest 扩展**：edit_file 唯一匹配成功；旧文本找不到报错；旧文本出现多次报错；grep 正常搜索返回格式正确；grep 超 200 条截断；glob 通配符匹配正确
- **InteractionToolsTest**：ConsoleUserInteraction 能读终端（mock stdin）；UnsupportedUserInteraction 抛异常
- **WebSearchToolsTest**：DuckDuckGoSearchProvider 返回结果格式正确；web_search 过 Sandbox HTTP_REQUEST 检查
- **IbiosToolContractTest**：参数化测试自动覆盖新增的 5 个工具（name/description/inputSchema 非空）

### Manual

Items requiring human verification — real model calls, real webhooks, etc.

- 真模型调一次 edit_file/grep/glob，确认 Agent 能正确组合使用
- 真模型调一次 ask_user（CLI 场景），确认终端交互正常
- 真模型调一次 web_search（DuckDuckGo），确认返回结果可用

---

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 16**: Profile/ProfileLoader/ProfileRegistry（ibios-core）、ProviderService（ibios-provider）、LlmCall+Repository（ibios-storage）
- **Lesson 17**: ReActLoop/PromptBuilder/ToolExecutor/AgentService/ProfileContext/ContextLoader（ibios-core）、ToolInvocation+Repository（ibios-storage）
- **Lesson 18**: Session/SessionManager（ibios-core/ibios-storage）
- **Lesson 19**: NotifyTools/NotifyChannelAdapter（ibios-tool/ibios-core）
- **Lesson 20（已交付部分）**: IbiosTool 接口、ToolRegistry、Sandbox 接口、FileTools/ShellTools/HttpTools

### External Dependencies

- DuckDuckGo Instant Answer API（免 key，HTTP GET）
- Spring AI Alibaba BOM（已锁定）

### Assumptions

- UserInteraction 接口在 ibios-core，ConsoleUserInteraction 在 ibios-tool
- SearchProvider 接口在 ibios-core，DuckDuckGoSearchProvider 在 ibios-tool
- edit_file 的"唯一匹配"语义与 Claude Code / Cursor 一致
- grep/glob 的 200 条上限足够覆盖绝大多数场景，超了就截断并注明

---

## Constitution Check

Verify this spec complies with `.specify/memory/constitution.md`:

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith
- [x] Principle 2: Five core capabilities priority（Tool 体系是能力四）
- [x] Principle 3: Self-implemented ReAct loop（不依赖 Spring AI Agent）
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED（只用 @Tool schema）
- [x] Principle 5: Plugin tool three-tier access（内置工具是基础，Plugin 扩展）
- [x] Principle 6: Synchronous + virtual threads（同步执行）
- [x] Principle 7: Sandbox interface-first（所有工具第一行过 Sandbox.enforce）
- [x] Principle 8: SQLite + audit day-one（工具执行写 tool_invocations）
- [x] Principle 9: Explicit provider mapping（不涉及 Provider）
- [x] Principle 10: Global invariants（Sandbox first、audit、no plaintext、no reactive、no auto-exec）

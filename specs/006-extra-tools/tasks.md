# Tasks: 5 个扩展内置工具

> Created by `/speckit-tasks`. Dependency-ordered, each task testable.

## Metadata

- **Feature**: 5 个扩展内置工具——对齐业界主流 Agent 产品的基础工具面
- **Plan**: `specs/006-extra-tools/plan.md`
- **Date**: 2026-10-14
- **Status**: pending

---

## Task Categories

- **[C]** — Configuration / Schema
- **[T]** — Test (harness)
- **[I]** — Implementation
- **[V]** — Verification

---

## Phase 1: Setup（项目初始化）

**Goal**: 确认依赖、创建包结构。

- [x] T001 验证 Spring AI Alibaba BOM 中存在 RestClient 依赖 `ibios-tool/pom.xml` `[C]`
  - **Depends on**: —
  - **Deliverables**: 确认 `mvn dependency:tree | grep restclient` 有输出
  - **Acceptance**: 依赖存在，无需新增

- [x] T002 创建 interaction 包结构 `ibios-tool/src/main/java/com/ibios/tool/interaction/` `[I]`
  - **Depends on**: —
  - **Deliverables**: 空目录（通过创建第一个类自动建立）
  - **Acceptance**: 目录存在

- [x] T003 创建 search 包结构 `ibios-tool/src/main/java/com/ibios/tool/search/` `[I]`
  - **Depends on**: —
  - **Deliverables**: 空目录（通过创建第一个类自动建立）
  - **Acceptance**: 目录存在

---

## Phase 2: Foundational（核心接口，阻塞后续 User Story）

**Goal**: 在 ibios-core 定义 UserInteraction 和 SearchProvider 接口。

- [x] T004 定义 UserInteraction 接口 `ibios-core/src/main/java/com/ibios/core/tool/UserInteraction.java` `[I]`
  - **Depends on**: T001
  - **Deliverables**: `UserInteraction` 接口，含 `String ask(String question)` 方法
  - **Acceptance**: 编译通过，接口定义清晰

- [x] T005 定义 SearchProvider 接口和 SearchResult record `ibios-core/src/main/java/com/ibios/core/tool/SearchProvider.java` `[I]`
  - **Depends on**: T001
  - **Deliverables**: `SearchProvider` 接口 + `SearchResult` record（title, url, snippet）
  - **Acceptance**: 编译通过，record 字段完整

---

## Phase 3: US1 — 文件工具扩展（edit_file / grep / glob）

**Story Goal**: 在 FileTools 中新增 3 个工具，覆盖局部编辑、内容搜索、文件查找。

**Independent Test Criteria**: FileToolsTest 扩展用例全绿，edit_file 唯一匹配约束验证通过，grep/glob 200 条上限验证通过。

### Tests（harness-first）

- [x] T006 [US1] 编写 edit_file 测试用例 `ibios-tool/src/test/java/com/ibios/tool/builtin/FileToolsTest.java` `[T]`
  - **Depends on**: T004, T005
  - **Deliverables**: 3 个测试方法：
    - `editFile_uniqueMatch_success`：唯一匹配成功替换
    - `editFile_notFound_error`：找不到报错，文件不动
    - `editFile_multipleMatches_error`：出现多次报错，文件不动
  - **Acceptance**: 测试编译通过（实现待 T007）

- [x] T007 [US1] 编写 grep 测试用例 `ibios-tool/src/test/java/com/ibios/tool/builtin/FileToolsTest.java` `[T]`
  - **Depends on**: T006
  - **Deliverables**: 3 个测试方法：
    - `grep_normalSearch_returnsResults`：正常搜索返回格式正确
    - `grep_overLimit_truncates`：超 200 条截断并注明
    - `grep_binaryFile_skips`：二进制文件跳过
  - **Acceptance**: 测试编译通过（实现待 T008）

- [x] T008 [US1] 编写 glob 测试用例 `ibios-tool/src/test/java/com/ibios/tool/builtin/FileToolsTest.java` `[T]`
  - **Depends on**: T007
  - **Deliverables**: 2 个测试方法：
    - `glob_patternMatch_returnsFiles`：通配符匹配正确
    - `glob_overLimit_truncates`：超 200 条截断
  - **Acceptance**: 测试编译通过（实现待 T009）

### Implementation

- [x] T009 [US1] 实现 EditFileTool `ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java` `[I]`
  - **Depends on**: T006
  - **Deliverables**: `EditFileTool` 内部类，实现 `IbiosTool` 接口
    - 首行 `sandbox.enforce(FILE_WRITE, filePath)`
    - 读取文件内容，检查 `old_text` 出现次数
    - 0 次 → 抛异常 "Text not found in file"
    - >1 次 → 抛异常 "Text appears N times at lines [...]"
    - 1 次 → 替换并写回文件
  - **Acceptance**: T006 的 3 个测试全绿

- [x] T010 [US1] 实现 GrepTool `ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java` `[I]`
  - **Depends on**: T007, T009
  - **Deliverables**: `GrepTool` 内部类，实现 `IbiosTool` 接口
    - 首行 `sandbox.enforce(FILE_READ, path)`
    - 递归搜索文件，按正则匹配
    - 跳过二进制/非 UTF-8 文件（catch exception, log warn, continue）
    - 收集结果，超 200 条截断并追加 "... (N more results truncated)"
    - 返回格式：`file:line_number:content`
  - **Acceptance**: T007 的 3 个测试全绿

- [x] T011 [US1] 实现 GlobTool `ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java` `[I]`
  - **Depends on**: T008, T010
  - **Deliverables**: `GlobTool` 内部类，实现 `IbiosTool` 接口
    - 首行 `sandbox.enforce(FILE_READ, path)`
    - 使用 `java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + pattern)`
    - 递归查找匹配文件
    - 超 200 条截断并追加 "... (N more results truncated)"
  - **Acceptance**: T008 的 2 个测试全绿

---

## Phase 4: US2 — 交互工具（ask_user）

**Story Goal**: 实现 ask_user 工具，支持 CLI 渠道读终端，其他渠道报不支持。

**Independent Test Criteria**: InteractionToolsTest 全绿，ConsoleUserInteraction 能读 stdin，UnsupportedUserInteraction 抛异常。

### Tests（harness-first）

- [x] T012 [US2] 编写 InteractionTools 测试用例 `ibios-tool/src/test/java/com/ibios/tool/interaction/InteractionToolsTest.java` `[T]`
  - **Depends on**: T004
  - **Deliverables**: 2 个测试方法：
    - `consoleInteraction_readsStdin`：mock stdin，验证读取一行
    - `unsupportedInteraction_throwsException`：验证抛 UnsupportedOperationException
  - **Acceptance**: 测试编译通过（实现待 T013, T014）

### Implementation

- [x] T013 [US2] 实现 ConsoleUserInteraction `ibios-tool/src/main/java/com/ibios/tool/interaction/ConsoleUserInteraction.java` `[I]`
  - **Depends on**: T012
  - **Deliverables**: 实现 `UserInteraction` 接口
    - `ask` 方法：打印问题到 stdout，从 stdin 读取一行
    - 使用 `BufferedReader(new InputStreamReader(System.in))`
  - **Acceptance**: T012 的 `consoleInteraction_readsStdin` 测试绿

- [x] T014 [US2] 实现 UnsupportedUserInteraction `ibios-tool/src/main/java/com/ibios/tool/interaction/UnsupportedUserInteraction.java` `[I]`
  - **Depends on**: T012
  - **Deliverables**: 实现 `UserInteraction` 接口
    - `ask` 方法：直接抛 `UnsupportedOperationException("User interaction not supported in this channel")`
  - **Acceptance**: T012 的 `unsupportedInteraction_throwsException` 测试绿

- [x] T015 [US2] 实现 AskUserTool `ibios-tool/src/main/java/com/ibios/tool/interaction/InteractionTools.java` `[I]`
  - **Depends on**: T013, T014
  - **Deliverables**: `InteractionTools` 类含 `AskUserTool` 内部类
    - 构造函数注入 `UserInteraction`
    - `execute` 方法：调用 `userInteraction.ask(question)`，返回结果
    - 无 Sandbox 检查（无外部 IO）
  - **Acceptance**: 手动验证 CLI 渠道能读终端

---

## Phase 5: US3 — 网页搜索工具（web_search）

**Story Goal**: 实现 web_search 工具，使用 DuckDuckGo Instant Answer API。

**Independent Test Criteria**: WebSearchToolsTest 全绿，DuckDuckGoSearchProvider 返回格式正确，Sandbox HTTP_REQUEST 检查通过。

### Tests（harness-first）

- [x] T016 [US3] 编写 WebSearchTools 测试用例 `ibios-tool/src/test/java/com/ibios/tool/search/WebSearchToolsTest.java` `[T]`
  - **Depends on**: T005
  - **Deliverables**: 2 个测试方法：
    - `duckDuckGoSearch_returnsResults`：mock HTTP 响应，验证解析正确
    - `webSearch_sandboxEnforcement`：验证首行调用 `sandbox.enforce(HTTP_REQUEST, "duckduckgo.com")`
  - **Acceptance**: 测试编译通过（实现待 T017, T018）

### Implementation

- [x] T017 [US3] 实现 DuckDuckGoSearchProvider `ibios-tool/src/main/java/com/ibios/tool/search/DuckDuckGoSearchProvider.java` `[I]`
  - **Depends on**: T016
  - **Deliverables**: 实现 `SearchProvider` 接口
    - 使用 `RestClient` 发送 GET 请求到 `https://api.duckduckgo.com/?q={query}&format=json`
    - 解析 JSON 响应，提取 `Abstract`、`AbstractURL`、`RelatedTopics`
    - 转换为 `SearchResult` 列表（最多 10 条）
    - 无结果返回空列表（不抛异常）
  - **Acceptance**: T016 的 `duckDuckGoSearch_returnsResults` 测试绿

- [x] T018 [US3] 实现 WebSearchTool `ibios-tool/src/main/java/com/ibios/tool/search/WebSearchTools.java` `[I]`
  - **Depends on**: T017
  - **Deliverables**: `WebSearchTools` 类含 `WebSearchTool` 内部类
    - 构造函数注入 `SearchProvider` 和 `Sandbox`
    - `execute` 方法：
      - 首行 `sandbox.enforce(HTTP_REQUEST, "duckduckgo.com")`
      - 调用 `searchProvider.search(query)`
      - 渲染结果为文本格式（编号列表，每项含 title/URL/snippet）
  - **Acceptance**: T016 的 `webSearch_sandboxEnforcement` 测试绿

---

## Phase 6: Polish & Cross-Cutting Concerns

**Goal**: 注册新工具、验证契约测试、全量构建。

- [x] T019 更新 ToolRegistry 注册新工具 `ibios-tool/src/main/java/com/ibios/tool/config/ToolConfiguration.java` `[I]`
  - **Depends on**: T009, T010, T011, T015, T018
  - **Deliverables**: 在配置类中注册 5 个新工具
    - `EditFileTool`, `GrepTool`, `GlobTool`（从 FileTools 获取）
    - `AskUserTool`（注入 `ConsoleUserInteraction` 或 `UnsupportedUserInteraction`，按渠道选择）
    - `WebSearchTool`（注入 `DuckDuckGoSearchProvider`）
  - **Acceptance**: Spring 容器启动成功，`ibios tool list` 可见 5 个新工具

- [x] T020 验证 IbiosToolContractTest 覆盖新增工具 `ibios-tool/src/test/java/com/ibios/tool/builtin/IbiosToolContractTest.java` `[V]`
  - **Depends on**: T019
  - **Deliverables**: 确认参数化测试自动覆盖 5 个新工具
    - 验证 name/description/inputSchema 非空
  - **Acceptance**: 测试全绿，5 个新工具都被遍历到

- [x] T021 运行全量构建验证 `mvn clean verify` `[V]`
  - **Depends on**: T020
  - **Deliverables**: 构建成功输出
  - **Acceptance**: 
    - BUILD SUCCESS
    - 无 P3C/SpotBugs/FindSecBugs/PMD 违规
    - 所有测试通过（含新增的 FileToolsTest/InteractionToolsTest/WebSearchToolsTest）

- [x] T022 H4 全局不变量自查 `[V]`
  - **Depends on**: T021
  - **Deliverables**: 5 条 grep 命令输出
  - **Acceptance**:
    - `grep -n "sandbox.enforce" ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java` → edit_file/grep/glob 首行都有
    - `grep -r "sk-" ibios-tool/src/main/java/` → 无输出
    - `grep -r "Mono\|Flux\|CompletableFuture" ibios-tool/src/main/java/com/ibios/tool/` → 无输出
    - `grep -r "autoExecuteTools" ibios-tool/src/main/java/` → 无输出
    - 确认 tool_invocations 审计正常（通过 ToolExecutor 既有路径）

---

## Deliverables Checklist

Cross-reference with lesson courseware's "本节交付物" section：

### Code

- [x] `UserInteraction` interface → `ibios-core/src/main/java/com/ibios/core/tool/UserInteraction.java`
- [x] `SearchProvider` interface + `SearchResult` record → `ibios-core/src/main/java/com/ibios/core/tool/SearchProvider.java`
- [x] `ConsoleUserInteraction` → `ibios-tool/src/main/java/com/ibios/tool/interaction/ConsoleUserInteraction.java`
- [x] `UnsupportedUserInteraction` → `ibios-tool/src/main/java/com/ibios/tool/interaction/UnsupportedUserInteraction.java`
- [x] `DuckDuckGoSearchProvider` → `ibios-tool/src/main/java/com/ibios/tool/search/DuckDuckGoSearchProvider.java`
- [x] `InteractionTools` (AskUserTool) → `ibios-tool/src/main/java/com/ibios/tool/interaction/InteractionTools.java`
- [x] `WebSearchTools` (WebSearchTool) → `ibios-tool/src/main/java/com/ibios/tool/search/WebSearchTools.java`
- [x] `EditFileTool` / `GrepTool` / `GlobTool` → expand `ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java`

### Tests

- [x] `FileToolsTest` (扩展 edit_file/grep/glob) → `ibios-tool/src/test/java/com/ibios/tool/builtin/FileToolsTest.java`
- [x] `InteractionToolsTest` → `ibios-tool/src/test/java/com/ibios/tool/interaction/InteractionToolsTest.java`
- [x] `WebSearchToolsTest` → `ibios-tool/src/test/java/com/ibios/tool/search/WebSearchToolsTest.java`
- [x] `IbiosToolContractTest` (参数化覆盖新增 5 工具) → `ibios-tool/src/test/java/com/ibios/tool/builtin/IbiosToolContractTest.java`

### Config

- [x] ToolConfiguration 注册 5 个新工具 → `ibios-tool/src/main/java/com/ibios/tool/config/ToolConfiguration.java`

---

## Task Summary

- **Total tasks**: 22
- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 2 tasks
- **Phase 3 (US1 - File tools)**: 6 tasks (3 tests + 3 implementations)
- **Phase 4 (US2 - Interaction)**: 4 tasks (1 test + 3 implementations)
- **Phase 5 (US3 - Web search)**: 3 tasks (1 test + 2 implementations)
- **Phase 6 (Polish)**: 4 tasks

**Parallel opportunities**:
- T002, T003 可并行（不同包）
- T004, T005 可并行（不同接口）
- T006, T007, T008 可并行（不同测试类，但同一文件，建议顺序）
- T013, T014 可并行（不同实现类）

**MVP scope**: Phase 1-3（edit_file/grep/glob）即可独立交付，验证文件工具扩展能力。

**Independent test criteria per story**:
- US1: FileToolsTest 扩展用例全绿
- US2: InteractionToolsTest 全绿
- US3: WebSearchToolsTest 全绿

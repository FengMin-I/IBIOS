# 任务清单：Tool 体系

> Feature: `specs/005-tool-system`
> 生成日期：2026-10-14

---

## Phase 1：Setup——依赖与接口地基

- [ ] T001 在 ibios-tool/pom.xml 添加 MCP SDK 依赖（`io.modelcontextprotocol:mcp`），在 parent pom.xml dependencyManagement 锁定版本
- [ ] T002 将 `ToolExecutionResult` 从 `ToolRegistry` 内部 record 提升为独立类 `com.ibios.core.tool.ToolExecutionResult`，补 `retryable` 字段和静态工厂方法 `success(output)` / `failure(msg, retryable)`
- [ ] T003 给 `IbiosTool` 接口补 `execute(String argumentsJson)` 方法，返回 `ToolExecutionResult`
- [ ] T004 重构 `ToolRegistry` 接口——`lookup` 返回 `IbiosTool`（而非 `IbiosToolLookup`），新增 `listAll()` / `filterForProfile(List<String>)` / `register(IbiosTool)` 方法；删除 `IbiosToolLookup` 内部接口
- [ ] T005 创建 `Sandbox` 接口 + `SandboxAction` record + `ActionType` 枚举 + `SandboxViolationException`（ibios-core）；创建 `NoopSandbox` 实现（ibios-tool，直接放行）
- [ ] T006 更新 `ToolExecutor` 适配新接口（`lookup` 返回 `IbiosTool`，`execute` 返回 `ToolExecutionResult`）

## Phase 2：Tests——harness 先行

- [ ] T007 [P] 写 `IbiosToolContractTest`：参数化测试遍历 ToolRegistry 里每个工具，断言 name/description/inputSchema 非空
- [ ] T008 [P] 写 `ToolRegistryTest`：三种来源工具以 IbiosTool 身份注册；按 Profile tools 字段过滤后子集精确匹配
- [ ] T009 [P] 写 `FileToolsTest`：read_file/write_file/list_dir 正常执行；路径越界（`../`）被拦
- [ ] T010 [P] 写 `ShellToolsTest`：白名单内命令能跑通；白名单外被拦
- [ ] T011 [P] 写 `HttpToolsTest`：GET/POST 正常取回响应（用 MockWebServer）；域名白名单外被拦
- [ ] T012 [P] 写 `McpClientServiceTest`：mock MCP server listTools 返回工具被包装注册；连接失败只 WARN、其余工具照常注册
- [ ] T013 [P] 写 `McpToolAdapterTest`：mock McpClient，execute 转发参数原样、结果包成 ToolExecutionResult；失败时 retryable=true

## Phase 3：ToolRegistry 实现 + SpringToolAdapter

- [ ] T014 实现 `DefaultToolRegistry`（ibios-tool）：ConcurrentHashMap 存储，register/lookup/listAll/filterForProfile
- [ ] T015 实现 `SpringToolAdapter`（ibios-tool）：把 Spring AI `@Tool` 注解的 Bean 方法包装成 `IbiosTool`——name/description/inputSchema 从 ToolCallback 取，execute 调 `callback.call(inputJson)`
- [ ] T016 创建 `ToolAutoConfiguration`（ibios-tool）：启动时收集所有 `IbiosTool` Bean + `@Tool` 注解方法，注册进 `DefaultToolRegistry`

## Phase 4：内置 Tool 实现

- [ ] T017 实现 `FileTools`（ibios-tool）：read_file / write_file / list_dir，execute 第一行调 `sandbox.enforce(...)`
- [ ] T018 实现 `ShellTools`（ibios-tool）：shell 命令执行，带超时，execute 第一行调 `sandbox.enforce(...)`
- [ ] T019 实现 `HttpTools`（ibios-tool）：http_get / http_post，用 RestClient，execute 第一行调 `sandbox.enforce(...)`
- [ ] T020 改造 `NotifyTools`：加 `@Tool` 注解，方法返回 `String`（而非 void），实现 `IbiosTool` 接口或包装成 `IbiosTool`

## Phase 5：MCP 集成

- [ ] T021 实现 `McpToolAdapter`（ibios-tool）：把 MCP Tool 适配成 `IbiosTool`，getName/getDescription/getInputSchema 映射 MCP tools/list 返回，execute 通过 MCP 协议转发
- [ ] T022 实现 `McpClientService`（ibios-tool）：启动时读 `.ibios/mcp_servers.yaml`，逐个连接 MCP server，调 tools/list 拿工具列表，包装注册；连接失败 WARN 跳过
- [ ] T023 创建 `.ibios/mcp_servers.yaml` 示例配置（init 命令自动生成）

## Phase 6：Profile tools 过滤 + ToolSchemaAdapter 改造

- [ ] T024 在 `AgentService` / `PromptBuilder` 里接入 Profile tools 过滤——Agent 启动时按 Profile.tools 从 ToolRegistry 过滤出可用子集
- [ ] T025 改造 `ToolSchemaAdapter`（ibios-provider）：适配新的 `IbiosTool` 接口（execute 方法已就位），但 `call()` 仍然 throw UnsupportedOperationException（Spring AI 自动执行保持禁用）

## Phase 7：验证与回归

- [ ] T026 跑 `mvn clean verify` 全量门禁，确认全绿（含 P3C/SpotBugs/FindSecBugs/PMD）
- [ ] T027 前序节全部测试回归绿（Lesson 16/17/18/19 测试无破坏）
- [ ] T028 H4 六条全局不变量逐条自查

---

## 依赖关系

```
Phase 1 (T001-T006) → Phase 2 (T007-T013, 可并行)
Phase 1 → Phase 3 (T014-T016)
Phase 1 + Phase 3 → Phase 4 (T017-T020)
Phase 1 + Phase 3 → Phase 5 (T021-T023)
Phase 4 + Phase 5 → Phase 6 (T024-T025)
Phase 6 → Phase 7 (T026-T028)
```

## 并行执行示例

- Phase 2 的 7 个测试任务可全部并行（不同测试文件，无交叉依赖）
- Phase 4 的 T017/T018/T019 可并行（不同工具类）
- Phase 5 的 T021/T022 顺序执行（T022 依赖 T021 的 McpToolAdapter）

## 实现策略

MVP 范围：Phase 1-3 + Phase 4（FileTools + HttpTools）+ Phase 6 的过滤 + Phase 7 = 最小可验证闭环。
MCP 集成（Phase 5）和 ShellTools 可在 MVP 验证后补齐。

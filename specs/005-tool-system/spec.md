# Feature Specification: Tool 体系——让 Agent 真正能动手干事

> Created by `/speckit-specify`。

## Metadata

- **Feature**: Tool 体系（IbiosTool 统一抽象 + 内置 Tool + MCP 集成 + ToolRegistry 实现）
- **Lesson**: 20
- **Author**: IBIOS Dev
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. 背景与价值

Provider 让 Agent 会调模型，ReAct 让它会思考，但到现在它还只会"想"和"说"。Tool 体系是让 Agent 真正能动手干事的那双手——LLM 决定"调哪个工具、传什么参数"，IBIOS 负责把工具真正执行掉、再把结果递回给 LLM。本节点立 `IbiosTool` 统一抽象（含 `getInputSchema`）、实现 `ToolRegistry` 具体注册表、交付 6 个内置 Tool（文件 3 + Shell 1 + HTTP 2）、完成 MCP Client 集成、补齐 `NotifyTools` 的 `@Tool` 注册。这是 Demo 一（每日天气）"真的去查天气"那个动作的落地前提。

## 2. 用户场景

### 场景 1：Agent 查天气给穿搭建议

用户问"今天天气怎么样，我该穿什么"，Agent 在 ReAct 循环里决定调 `http_get` 查天气 API，IBIOS 的 `HttpTools` 执行请求、过域名白名单、返回结果，Agent 再根据天气数据给出穿搭建议。

### 场景 2：业务方通过 MCP 接入自定义工具

企业运维团队写了一个 Kubernetes MCP server，在 `.ibios/mcp_servers.yaml` 里声明连接信息。IBIOS 启动时 `McpClientService` 自动连上、拿到工具列表、包装成 `IbiosTool` 注册进 `ToolRegistry`。Agent 在对话中可以直接调用这些工具查 Pod 状态、重启服务。某个 MCP server 失联时只记 WARN，不影响其他工具正常注册和启动。

### 场景 3：业务方通过 Java @Tool 写深度集成工具

Java 开发者用 Spring AI `@Tool` 注解标一个 Spring Bean 方法，IBIOS 启动时自动扫描、生成 schema、包装成 `IbiosTool` 注册。工具直接在进程内调用 Java 方法，不走 MCP 协议，性能最好，适合需要调用企业内部 Java 服务的场景。

## 3. 功能需求

- **FR1**: `IbiosTool` 接口补全 `execute` 方法——接收 JSON 输入、返回 `ToolResult`（成功标识、结果内容、错误信息、是否可重试）。现有接口只有 `name`/`description`/`inputSchema` 三个方法，需补 `execute`。
- **FR2**: `ToolRegistry` 具体实现——启动时收集所有 `IbiosTool` 实例（内置 Tool + `@Tool` 注解扫描 + MCP 适配），提供按名查找和按 Profile `tools` 字段过滤子集的能力。
- **FR3**: `FileTools` 三个内置工具——`read_file`（读文件）、`write_file`（写文件）、`list_dir`（列目录），执行前调用 `Sandbox.enforce(...)` 做路径白名单检查（Sandbox 本体 23/24 节交付，本节先留调用位）。
- **FR4**: `ShellTools` 一个内置工具——`shell`（跑命令），执行前过命令白名单检查，带超时。
- **FR5**: `HttpTools` 两个内置工具——`http_get`（GET 请求）、`http_post`（POST 请求），执行前过域名白名单检查。
- **FR6**: `McpClientService`——启动时读 `.ibios/mcp_servers.yaml`，连接每个 MCP server，调 `tools/list` 拿工具列表，把每个工具包装成 `McpToolAdapter`（实现 `IbiosTool`）注册进 `ToolRegistry`。连接失败的 server 记 WARN 跳过，不拖垮启动。
- **FR7**: `McpToolAdapter`——把 MCP Tool 适配成 `IbiosTool` 接口。`getName`/`getDescription`/`getInputSchema` 直接映射 MCP 的 `tools/list` 返回，`execute` 通过 MCP 协议转发给 MCP server 执行，结果包成 `ToolResult`。
- **FR8**: `NotifyTools` 补全 `@Tool` 注解注册，方法返回 `String`（而非 void），使其成为合格的 `IbiosTool`。
- **FR9**: Profile 的 `tools` 字段过滤——Agent 启动时按 Profile 声明的 `tools` 列表从 `ToolRegistry` 过滤出可用子集，断言恰好等于声明列表（不多不少）。
- **FR10**: `ToolResult` 包含五要素：`toolName`、`inputJson`、`output`、`success`、`errorMessage`、`retryable`（是否可重试）。现有 `ToolResult` 缺 `retryable` 字段。

## 4. 明确不做（边界）

- Tool Policy（哪个 Agent 能用哪些工具的 allow/deny 规则）——扩展阶段
- 工具太多时的按需加载——扩展阶段
- 把 IBIOS 自己变成 MCP server 对外暴露——扩展阶段
- 完整的容器级沙箱（Sandbox 本体）——23/24 节交付，本节只留调用位
- 一次响应里多个工具并行调用——顺序执行
- `EmailNotifyAdapter`（SMTP 专用实现）——扩展阶段

## 5. 验收标准

### 自动化（Harness）

| 测试类 | 覆盖点 |
|---|---|
| `IbiosToolContractTest` | **参数化测试遍历 Registry 里每个工具**：name/description/inputSchema 都非空——任何一个工具漏实现 `getInputSchema()`，这里立刻红 |
| `ToolRegistryTest` | 三种来源的工具都以 `IbiosTool` 身份注册进来；按 Profile 的 `tools` 字段过滤后，子集精确匹配、不多不少 |
| `FileToolsTest` | 读/写/列目录正常能跑通；路径越界（`../` 穿越）被拦下 |
| `ShellToolsTest` | 白名单内命令能跑通；白名单外命令被拦下 |
| `HttpToolsTest` | GET/POST 正常能取回响应；域名白名单外被拦下 |
| `McpClientServiceTest` | mock MCP server：listTools 返回的工具被包装注册；**连接失败只 WARN、其余工具照常注册、启动不炸** |
| `McpToolAdapterTest` | mock `McpClient`：execute 转发参数原样、结果包成 `ToolResult`；失败时 retryable=true |

关键回归点：契约三件套（name/description/inputSchema 非空）、Profile 过滤精确匹配、MCP 失联隔离、白名单拦截。

### 人工项

- 方式一真跑一次：写一份 SKILL.md + 连一个真实 MCP server，Agent 能读懂意图并调用外部工具完成任务（依赖真模型和真 server）
- 方式三真跑一次：`@Tool` 示例工具在 `tool list` 里可见、Agent 能调通
- 每次工具调用写进 `tool_invocations`（17 节 ToolExecutorTest 已覆盖，跑真链路时顺带目检）

## 6. 依赖与假设

### 前序节依赖

- **Lesson 16**: Profile/ProfileLoader/ProfileRegistry（ibios-core）、ProviderService（ibios-provider）
- **Lesson 17**: ReActLoop/ToolExecutor/AgentService（ibios-core）、ToolInvocation 审计（ibios-storage）
- **Lesson 19**: NotifyChannelAdapter/WebhookNotifyAdapter（ibios-tool）

### 外部依赖

- Spring AI Alibaba BOM（`@Tool` 注解 + schema 生成）
- MCP Java SDK（MCP Client 集成）
- `.ibios/mcp_servers.yaml` 配置文件

---

## Constitution Check

- [x] 原则 1: JDK 21 + Spring Boot 3.x 单体
- [x] 原则 2: 五大核心能力优先（Tool 体系 = 核心能力四）
- [x] 原则 3: 自实现 ReAct 循环（Tool 执行由 IBIOS ToolExecutor 控制）
- [x] 原则 4: Spring AI 只用 Provider 抽象 + @Tool schema 生成，禁用自动 tool 执行
- [x] 原则 5: Plugin Tool 三档接入
- [x] 原则 6: 同步 + 虚拟线程
- [x] 原则 7: Sandbox 接口先行（本节留调用位，23/24 节交付本体）
- [x] 原则 8: SQLite + 审计 Day One（tool_invocations 已有）
- [x] 原则 9: 显式 Provider 映射
- [x] 原则 10: 全局不变量（无明文密钥、无 Reactor、无 Spring AI 自动工具执行）

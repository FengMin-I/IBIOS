# Implementation Plan: Tool 体系

> Created by `/speckit-plan`。

## Metadata

- **Feature**: Tool 体系（IbiosTool 统一抽象 + 内置 Tool + MCP 集成 + ToolRegistry 实现）
- **Spec**: `specs/005-tool-system/spec.md`
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. 技术栈

JDK 21 + Spring Boot 3.x + Spring AI Alibaba（动手前先跑 `mvn dependency:tree` 确认锁定 BOM 里目标依赖存在）+ SQLite + Spring Data JPA。凭证走环境变量占位，不落明文。SQLite 用手工建表脚本，不依赖 `hibernate.ddl-auto=update`。

## 2. 模块落位

| 类 / 包 | 目标模块 |
|---------|---------|
| `IbiosTool` 接口（补 `execute` 方法） | ibios-core |
| `ToolResult` record（补 `retryable` 字段） | ibios-core |
| `ToolRegistry` 接口 + `DefaultToolRegistry` 实现 | ibios-core（接口）+ ibios-tool（实现） |
| `FileTools`（read_file / write_file / list_dir） | ibios-tool |
| `ShellTools`（shell） | ibios-tool |
| `HttpTools`（http_get / http_post） | ibios-tool |
| `McpClientService` | ibios-tool |
| `McpToolAdapter` | ibios-tool |
| `NotifyTools`（补 @Tool 注解） | ibios-tool |
| `ToolSchemaAdapter` 改造（对接 execute） | ibios-provider |
| `IbiosToolContractTest` | ibios-tool (test) |
| `ToolRegistryTest` | ibios-tool (test) |
| `FileToolsTest` / `ShellToolsTest` / `HttpToolsTest` | ibios-tool (test) |
| `McpClientServiceTest` / `McpToolAdapterTest` | ibios-tool (test) |

## 3. 设计决策

### 决策 1：IbiosTool 接口补 execute，统一内置与 MCP

现有 `IbiosTool` 只有 name/description/inputSchema 三个方法，execute 在 `ToolRegistry.IbiosToolLookup` 内部接口里。Lesson 20 把 execute 提升到 `IbiosTool` 接口本身，让任何来源的工具都是完整的"描述 + 执行"单元。`ToolRegistry` 简化为纯粹的注册表 + 按名查找 + 按 Profile 过滤。

### 决策 2：@Tool 注解方法的适配策略

Spring AI `@Tool` 注解标在 Java 方法上，框架自动扫描生成 JSON Schema。IBIOS 需要一个 `SpringToolAdapter` 把 Spring AI 的 `ToolCallback` 包装成 `IbiosTool`——name/description/inputSchema 从 callback 取，execute 调 `callback.call(inputJson)`。这样内置 Tool 和方式三 Plugin Tool 走同一条注册路径。

### 决策 3：MCP 失联隔离

`McpClientService.connectAll()` 逐个连接 MCP server，每个连接用 try-catch 包裹。连接失败的 server 记 WARN 日志、跳过它的所有工具，不影响其他 server 的正常注册和整体启动。外部依赖的可用性不是自身的可用性。

### 决策 4：Sandbox 调用位先留

FileTools/ShellTools/HttpTools 的 execute 方法开头调 `sandbox.enforce(...)`，但 Sandbox 接口和 WhitelistSandbox 实现是 23/24 节的交付物。本节先定义 `Sandbox` 接口（`enforce(SandboxAction)` 方法），留一个 `NoopSandbox`（直接放行）作为默认实现，23/24 节替换为 `WhitelistSandbox`。

## 4. 测试策略

测试策略按课件"验收 harness"执行：`IbiosToolContractTest`、`ToolRegistryTest`、`FileToolsTest`、`ShellToolsTest`、`HttpToolsTest`、`McpClientServiceTest`、`McpToolAdapterTest`（覆盖契约三件套非空、Profile 过滤精确匹配、MCP 失联隔离、白名单拦截、文件/Shell/HTTP 正常执行），单测默认跑、集成冒烟打 `@Tag("integration")` CI 跳过；实现完成的定义是 `mvn clean verify` 全绿。

## 5. 语法禁区

避开 P3C/ASM 解析不了的 Java 18+ 语法形态（如增强 switch 的 `default ->` 写法），静态检查是构建门禁。

## 6. Constitution Check

- [x] 所有原则已从 spec 验证
- [x] 模块落位遵循 §10 依赖方向（ibios-tool 依赖 ibios-core，ibios-core 不依赖 Spring）
- [x] 无 Reactor 导入
- [x] 无 Spring AI 自动工具执行（autoExecuteTools = false）
- [x] 审计表已有（tool_invocations，Lesson 17 交付）
- [x] Sandbox 执行路径标注（本节留调用位 + NoopSandbox，23/24 节交付 WhitelistSandbox）

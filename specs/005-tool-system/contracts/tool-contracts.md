# Contracts: Tool 体系

## 1. IbiosTool 接口契约

所有工具（内置、@Tool 注解、MCP 适配）必须实现此接口：

```java
public interface IbiosTool {
    /** 工具名，LLM 靠它点名要调谁。非空。 */
    String name();
    
    /** 工具描述，给 LLM 看。非空。 */
    String description();
    
    /** JSON Schema 格式的参数说明。非空。 */
    String inputSchema();
    
    /** 执行工具。输入 JSON 字符串，返回执行结果。 */
    ToolExecutionResult execute(String argumentsJson);
}
```

**契约不变量**：`name()`、`description()`、`inputSchema()` 三个方法返回值不得为 null。`IbiosToolContractTest` 参数化测试遍历 Registry 里每个工具验证此不变量。

## 2. ToolExecutionResult 契约

```java
public record ToolExecutionResult(
    String output,        // 成功时的结果内容
    boolean success,      // 是否成功
    String errorMessage,  // 失败时的错误信息
    boolean retryable     // 是否可重试
) {
    public static ToolExecutionResult success(String output);
    public static ToolExecutionResult failure(String errorMessage, boolean retryable);
}
```

## 3. ToolRegistry 契约

```java
public interface ToolRegistry {
    /** 按名查找工具。找不到返回 null。 */
    IbiosTool lookup(String name);
    
    /** 列出所有已注册工具。 */
    List<IbiosTool> listAll();
    
    /** 按 Profile 的 tools 字段过滤。返回恰好等于声明列表的子集（不多不少）。 */
    List<IbiosTool> filterForProfile(List<String> toolNames);
    
    /** 注册一个工具。 */
    void register(IbiosTool tool);
}
```

**三种来源统一注册**：
1. 内置 Tool（FileTools/ShellTools/HttpTools/NotifyTools）——Spring Bean 实现 `IbiosTool`，启动时自动注册
2. @Tool 注解 Plugin Tool——`SpringToolAdapter` 把 Spring AI `ToolCallback` 包装成 `IbiosTool`
3. MCP Tool——`McpToolAdapter` 把 MCP 工具包装成 `IbiosTool`

## 4. 内置 Tool 契约

### FileTools

| 工具名 | 参数 | 行为 |
|--------|------|------|
| `read_file` | `{"path": "..."}` | 读文件内容，执行前 `sandbox.enforce(FILE_READ, path)` |
| `write_file` | `{"path": "...", "content": "..."}` | 写文件，执行前 `sandbox.enforce(FILE_WRITE, path)` |
| `list_dir` | `{"path": "..."}` | 列目录，执行前 `sandbox.enforce(FILE_READ, path)` |

### ShellTools

| 工具名 | 参数 | 行为 |
|--------|------|------|
| `shell` | `{"command": "...", "args": [...]}` | 执行命令，执行前 `sandbox.enforce(SHELL_COMMAND, command)`，带超时 |

### HttpTools

| 工具名 | 参数 | 行为 |
|--------|------|------|
| `http_get` | `{"url": "..."}` | GET 请求，执行前 `sandbox.enforce(HTTP_REQUEST, url)` |
| `http_post` | `{"url": "...", "body": "..."}` | POST 请求，执行前 `sandbox.enforce(HTTP_REQUEST, url)` |

## 5. MCP 集成契约

### McpClientService

- 启动时读 `.ibios/mcp_servers.yaml`
- 逐个连接 MCP server，调 `tools/list` 拿工具列表
- 每个 MCP 工具包装成 `McpToolAdapter` 注册进 `ToolRegistry`
- **连接失败的 server 记 WARN 跳过，不拖垮启动**

### McpToolAdapter

- `name()`/`description()`/`inputSchema()` 映射 MCP `tools/list` 返回
- `execute(input)` 通过 MCP 协议转发给 MCP server
- 调用失败返回 `ToolExecutionResult.failure(msg, true)`（retryable=true）

## 6. Sandbox 调用契约

FileTools/ShellTools/HttpTools 的 `execute` 方法**第一行**调用 `sandbox.enforce(...)`。校验失败抛 `SandboxViolationException`，走 ToolExecutor 已有的失败审计路径。

本节交付 `Sandbox` 接口 + `NoopSandbox`（直接放行）。`WhitelistSandbox` 由 23/24 节交付。

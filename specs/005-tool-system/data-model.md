# Data Model: Tool 体系

## 实体变更

本节点不新增数据库表。`tool_invocations` 表已在 Lesson 17 交付，Tool 执行审计复用现有路径。

## 接口变更

### IbiosTool 接口（ibios-core）

现有接口只有三个方法，补 `execute`：

```java
public interface IbiosTool {
    String name();
    String description();
    String inputSchema();  // JSON Schema string
    ToolExecutionResult execute(String argumentsJson);
}
```

### ToolResult / ToolExecutionResult（ibios-core）

现有 `ToolResult` record 在 `session` 包，用于 ReAct 循环累积。新增 `ToolExecutionResult` 用于 `IbiosTool.execute` 返回值：

```java
public record ToolExecutionResult(String output, boolean success, String errorMessage, boolean retryable) {
    public static ToolExecutionResult success(String output) { ... }
    public static ToolExecutionResult failure(String errorMessage, boolean retryable) { ... }
}
```

### ToolRegistry（ibios-core 接口 + ibios-tool 实现）

```java
// ibios-core/port
public interface ToolRegistry {
    IbiosTool lookup(String name);
    List<IbiosTool> listAll();
    List<IbiosTool> filterForProfile(List<String> toolNames);
    void register(IbiosTool tool);
}
```

### Sandbox 接口（ibios-core）

```java
public interface Sandbox {
    void enforce(SandboxAction action);
}

public record SandboxAction(ActionType type, String target) {}

public enum ActionType {
    FILE_READ, FILE_WRITE, SHELL_COMMAND, HTTP_REQUEST
}
```

`NoopSandbox`（ibios-tool）：直接放行，23/24 节替换为 `WhitelistSandbox`。

## 配置

### .ibios/mcp_servers.yaml

```yaml
servers:
  - name: weather-mcp
    transport: stdio
    command: npx -y @modelcontextprotocol/server-weather
    env: {}
  - name: custom-mcp
    transport: sse
    url: http://localhost:3001/mcp
    env: {}
```

### Profile tools 字段

```yaml
name: weather-agent
tools:
  - http_get
  - http_post
  - read_file
  - notify
```

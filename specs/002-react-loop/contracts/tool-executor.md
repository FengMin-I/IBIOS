# Contract: ToolExecutor

## 职责

执行 LLM 返回的工具调用请求。从 ToolRegistry 找到工具，执行沙箱检查，执行工具，返回 ToolResult，同时写入 tool_invocations 审计表。

## 接口

```java
public class ToolExecutor {
    public ToolResult execute(String sessionId, ToolCall call);
}
```

## 行为契约

1. 从 ToolRegistry 按名称查找工具
2. 执行沙箱检查（Sandbox 未就位时留调用位，注明第 24 节接线）
3. 执行工具，捕获异常
4. 写入 tool_invocations 审计表（成功 success=true，失败 success=false + error_message）
5. 返回 ToolResult（含输出文本或错误信息）

## 依赖

- ToolRegistry（ibios-tool，本节不实现，留接口调用位）
- Sandbox（ibios-tool，本节不实现，留调用位）
- ToolInvocationAudit（ibios-storage）

## 异常

- 工具未找到：抛出 ToolNotFoundException
- 工具执行异常：捕获后记录审计，返回含错误信息的 ToolResult（不吞异常）

# Contract: ReActLoop

## 职责

Agent 核心循环引擎。接收 Session + 用户消息 + Profile，输出最终响应文本。

## 接口

```java
public class ReActLoop {
    public String run(Session session, String userMessage, Profile profile);
}
```

## 行为契约

1. 将用户消息追加到 Session 对话历史
2. 循环执行（最多 profile.maxIterations() 次）：
   - 调用 PromptBuilder.build(session, profile) 组装 Prompt
   - 调用 ProviderService.chat(session.id(), profile, prompt) 获取响应
   - 将响应追加到 Session
   - 如果响应没有工具调用，返回响应文本
   - 如果有工具调用，逐个调用 ToolExecutor.execute(session.id(), call) 执行
   - 将工具结果追加到 Session
3. 达到最大轮数时返回"达到最大轮数，已停止"

## 依赖

- PromptBuilder（同模块）
- ProviderService（ibios-provider）
- ToolExecutor（同模块）
- Session（ibios-core）
- Profile（ibios-core）

## 异常

- 不抛出受检异常
- Provider 调用失败时抛出 ProviderException（由 ProviderService 定义）

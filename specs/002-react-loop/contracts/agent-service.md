# Contract: AgentService

## 职责

三种触发源（CLI/Web Service/定时任务）的统一入口。一次处理的编排者。

## 接口

```java
public class AgentService {
    public String process(Session session, String userMessage);
}
```

## 行为契约

1. 从 ProfileRegistry 获取 session 对应的 Profile
2. 将 Profile 放入 ProfileContext（ThreadLocal）
3. 调用 ReActLoop.run(session, userMessage, profile)
4. 持久化 Session（调用 SessionManager.save）
5. finally 块中清除 ProfileContext
6. 返回最终响应文本

## 依赖

- ProfileRegistry（ibios-core）
- ProfileContext（同模块）
- ReActLoop（同模块）
- SessionManager（接口，ibios-core）

## 异常

- Profile 未找到：抛出 ProviderNotFoundException（Lesson 16 已定义）
- ReActLoop 执行异常：上抛，但 finally 仍清除 ProfileContext

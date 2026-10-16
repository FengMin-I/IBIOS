# Contracts: AgentScheduler 定时触发

## Contract 1: AgentScheduler 公共接口

```java
@Component
public class AgentScheduler {
    // 启动时扫描所有 Profile 并注册定时任务
    @PostConstruct
    public void registerAll();

    // 单次执行（包级可见，供测试直接调用）
    void runOnce(Profile profile, Profile.ScheduleConfig config);

    // 获取指定任务的锁（供测试模拟锁占用）
    Lock lockFor(String taskId);
}
```

## Contract 2: ScheduleConfig 字段约定

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | String | 是 | — | 任务唯一标识 |
| cron | String | 是 | — | Spring 6 位 cron 表达式 |
| zone | String | 否 | "UTC" | IANA 时区 ID |
| message | String | 是 | — | 触发时发给 Agent 的消息 |

## Contract 3: 会话身份约定

定时触发的 Session 三元组固定为：
- channel = `"scheduler"`
- userId = `"scheduler"`
- profileName = `profile.name()`

同一 Profile 的历次定时触发复用同一 Session。

## Contract 4: 失败隔离约定

- `runOnce` 内部 catch `Exception`，只记 `log.error`，不外抛
- `finally` 块必须 `unlock`，保证锁释放
- 失败的审计走 `AgentService.process` 内部已有路径（`llm_calls` / `tool_invocations`）

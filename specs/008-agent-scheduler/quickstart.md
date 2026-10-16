# Quickstart: AgentScheduler 定时触发

## 前置条件

- Lesson 16-18 代码已就位（ProfileRegistry、AgentService、SessionManager）
- 至少一个 Agent 目录含 `schedules` 配置

## 验证步骤

### 1. 单元测试

```bash
mvn test -pl ibios-core -Dtest=AgentSchedulerTest
```

预期：4 个测试全绿（cron+时区、重叠跳过、失败隔离+锁释放、会话身份）。

### 2. 全量门禁

```bash
mvn clean verify
```

预期：BUILD SUCCESS，所有模块测试通过。

### 3. 人工验证（需真实 LLM）

1. 在 `.ibios/agents/` 下创建一个 Agent，frontmatter 含：
   ```yaml
   schedules:
     - id: test-every-minute
       cron: "0 * * * * *"
       zone: "Asia/Shanghai"
       message: "你好，请回复当前时间"
   ```
2. 启动应用，等待到点触发
3. 检查日志确认 Agent 自动发起了对话
4. 检查 `llm_calls` 表有对应审计记录

## 关键回归点

| 测试方法 | 守住的坑 |
|----------|----------|
| cronTriggerIncludesExpressionAndZone | 时区：不让服务器时区替用户做主 |
| previousRunStillHoldingLock_skipsTrigger | 重叠执行 |
| exceptionInProcess_doesNotThrowAndLockReleased | 失败隔离 + 不留死锁 |
| sessionIdentityFixedAs_scheduler_scheduler_profileName | 会话身份约定 |

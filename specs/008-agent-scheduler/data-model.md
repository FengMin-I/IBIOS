# Data Model: AgentScheduler 定时触发

## 实体变更

### Profile.ScheduleConfig（修改）

当前：`ScheduleConfig(String cron, String zone, String message)`

目标：`ScheduleConfig(String id, String cron, String zone, String message)`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 任务唯一标识，用于锁维度和日志 |
| cron | String | 标准 cron 表达式（Spring 6 位格式） |
| zone | String | 时区 ID（如 "Asia/Shanghai"） |
| message | String | 触发时发给 Agent 的消息内容 |

### AGENT.md frontmatter 示例

```yaml
schedules:
  - id: morning-weather
    cron: "0 0 9 * * *"
    zone: "Asia/Shanghai"
    message: "请查询今天的天气并推送给我"
  - id: weekly-report
    cron: "0 0 17 * * FRI"
    zone: "Asia/Shanghai"
    message: "请生成本周工作总结"
```

## 运行时结构

### AgentScheduler 内部状态

| 字段 | 类型 | 说明 |
|------|------|------|
| taskScheduler | ThreadPoolTaskScheduler | Spring 调度器 |
| profileRegistry | ProfileRegistry | Agent 配置注册表 |
| agentService | AgentService | 统一处理入口 |
| sessionManager | SessionManager | 会话管理 |
| taskLocks | ConcurrentMap<String, Lock> | 按任务 id 维度的防重叠锁 |

## 不涉及新表

核心阶段定时任务状态不持久化（第 28 节补 `scheduled_tasks` / `task_executions` 表）。

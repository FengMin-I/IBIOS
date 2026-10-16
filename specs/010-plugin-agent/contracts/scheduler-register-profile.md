# AgentScheduler registerProfile 契约

## 新增方法

### `void registerProfile(Profile profile)`

**行为**：
1. 遍历 `profile.schedules()`
2. 对每个 `ScheduleConfig`：
   - 计算 nextRun
   - 在 `taskStore` 注册 `ScheduledTaskView`
   - 用 `taskScheduler.schedule()` 注册 cron 任务
   - 把返回的 `ScheduledFuture<?>` 存入 `scheduledTasks` map，key = schedule.id
3. LOG.info 注册结果

**幂等性**：同一 profile 重复调用会注册两次定时（第 30 节在调用前先 unregister）。本节不要求幂等。

### `Map<String, ScheduledFuture<?>> scheduledTasks()`

**行为**：返回 `scheduledTasks` 的不可变视图。

## 既有方法（不变）

- `void registerAll()`：`@PostConstruct`，遍历 `profileRegistry.getAll()` 调 `registerProfile`
- `void runNow(String taskId)`：立即执行指定任务
- `void runOnce(Profile, ScheduleConfig)`：执行一次

## 测试锚点

- `AgentSchedulerRegisterTest`：registerProfile 后 scheduledTasks 有句柄、cron/时区来自 Profile.schedules

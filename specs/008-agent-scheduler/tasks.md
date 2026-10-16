# Tasks: AgentScheduler 定时触发

> Created by `/speckit-tasks`. Dependency-ordered, each task testable.

## Metadata

- **Feature**: AgentScheduler 定时触发（Lesson 25）
- **Plan**: `specs/008-agent-scheduler/plan.md`
- **Date**: 2026-10-15
- **Status**: pending

---

## Task Categories

- **[C]** — Configuration / Schema
- **[T]** — Test (harness)
- **[I]** — Implementation
- **[V]** — Verification

## Task List

### Phase 1: 基础改造——ScheduleConfig 补 id 字段

> 所有后续任务的前置依赖。ScheduleConfig 从三字段变四字段，ProfileLoader 解析和已有测试同步改。

- [ ] T001 [C] 修改 `Profile.ScheduleConfig` record 新增 `String id` 作为第一个字段，签名变为 `(String id, String cron, String zone, String message)` in `ibios-core/src/main/java/com/ibios/core/profile/Profile.java`
- [ ] T002 [I] 修改 `ProfileLoader.convertSchedules()` 读取 YAML map 中的 `id` 字段，传入 `ScheduleConfig` 构造器第一个参数 in `ibios-core/src/main/java/com/ibios/core/profile/ProfileLoader.java`
- [ ] T003 [T] 更新 `ProfileLoaderTest` 中涉及 `ScheduleConfig` 的构造调用和断言，补充 id 字段解析的测试用例 in `ibios-core/src/test/java/com/ibios/core/profile/ProfileLoaderTest.java`

### Phase 2: US1——AgentScheduler 核心实现 + 验收 harness

> User Story 1：定时触发作为第三种触发源（钟推），到点自动发起 ReAct 循环。
>
> **独立可测标准**：`AgentSchedulerTest` 4 个测试全绿 + `mvn clean verify` 全绿。

#### 测试先行（harness）

- [ ] T004 [T] [US1] 创建 `AgentSchedulerTest`，包含四个关键回归测试方法（方法名英文，`@DisplayName` 中文）：
  1. `cronTriggerIncludesExpressionAndZone` — ArgumentCaptor 抓 `CronTrigger` 注册参数，断言 cron 表达式和时区都传入了
  2. `previousRunStillHoldingLock_skipsTrigger` — 手动锁住任务锁后调 `runOnce`，verify `agentService.process` 未被调用
  3. `exceptionInProcess_doesNotThrowAndLockReleased` — mock `agentService.process` 抛异常，断言不外抛 + 再调一次 `runOnce` 证明锁已释放
  4. `sessionIdentityFixedAs_scheduler_scheduler_profileName` — 两次调 `runOnce`，ArgumentCaptor 捕获 `sessionManager.getOrCreate` 参数，断言 channel="scheduler"、userId="scheduler"、profileName=profile.name()

  文件：`ibios-core/src/test/java/com/ibios/core/react/AgentSchedulerTest.java`（与 `AgentScheduler` 同包，访问包级可见方法 `runOnce`/`lockFor`）

#### 核心实现

- [ ] T005 [I] [US1] 实现 `AgentScheduler` 类 in `ibios-core/src/main/java/com/ibios/core/react/AgentScheduler.java`：
  - `@Component` 注解
  - 依赖注入：`ThreadPoolTaskScheduler`、`ProfileRegistry`、`AgentService`、`SessionManager`
  - `ConcurrentMap<String, Lock> taskLocks = new ConcurrentHashMap<>()`
  - `@PostConstruct registerAll()`：遍历 `profileRegistry.getAll()`，对每个 Profile 的每条 `ScheduleConfig`，用 `taskScheduler.schedule(() -> runOnce(profile, sc), new CronTrigger(sc.cron(), TimeZone.getTimeZone(ZoneId.of(sc.zone()))))` 动态注册
  - `void runOnce(Profile profile, ScheduleConfig sc)`（包级可见）：
    1. `taskLocks.computeIfAbsent(sc.id(), id -> new ReentrantLock())`
    2. `if (!lock.tryLock()) { log skip; return; }`
    3. try 块：`sessionManager.getOrCreate("scheduler", "scheduler", profile.name())` → `agentService.process(session, sc.message())`
    4. catch `Exception`：`log.error("scheduled task {} failed", sc.id(), e)`
    5. finally：`lock.unlock()`
  - `Lock lockFor(String taskId)`（包级可见）：`taskLocks.computeIfAbsent(taskId, id -> new ReentrantLock())`，供测试模拟锁占用

- [ ] T006 [I] [US1] 确认 `ThreadPoolTaskScheduler` Bean 可用：检查 `ibios-boot` 的 `application.yaml` 或配置类是否已启用 `@EnableScheduling`；若未启用，在 `ibios-boot` 的配置类上加 `@EnableScheduling`（Spring Boot 自动提供 `ThreadPoolTaskScheduler` Bean，无需手动声明）

### Phase 3: 验证与门禁

- [ ] T007 [V] 运行 `mvn test -pl ibios-core -Dtest=AgentSchedulerTest`，确认 4 个测试全绿
- [ ] T008 [V] 运行 `mvn clean verify`，确认全模块 BUILD SUCCESS（含 P3C/SpotBugs/PMD 静态检查）
- [ ] T009 [V] 存在性核对：`AgentScheduler.java`、`AgentSchedulerTest.java`、`Profile.ScheduleConfig` 四字段签名、`ProfileLoader.convertSchedules` 读 id

---

## Deliverables Checklist

对照课件"本节交付物"：

- [x] 代码：`AgentScheduler`（`registerAll` + `runOnce` + 按任务 id 的 `ReentrantLock` 表）→ T005
- [x] 代码：`ScheduleConfig`（id/cron/zone/message）→ T001
- [x] 测试：`AgentSchedulerTest`（4 个关键回归点）→ T004
- [x] 配置：Profile `schedules` 字段（已有，补 id 解析）→ T002
- [x] 约定：会话身份固定 `("scheduler", "scheduler", profileName)` → T005 runOnce 内实现
- [x] 约定：失败只记日志不崩调度器 → T005 runOnce catch+finally 实现

---

## Dependencies

```
T001 (ScheduleConfig id) → T002 (ProfileLoader) → T003 (ProfileLoaderTest)
                                                    ↓
                                              T004 (AgentSchedulerTest)
                                                    ↓
                                              T005 (AgentScheduler 实现)
                                                    ↓
                                              T006 (@EnableScheduling 确认)
                                                    ↓
                                              T007-T009 (验证)
```

## Parallel Execution

- T001 + T002 + T003 串行（同一文件链）
- T004 可在 T003 完成后立即开始（与 T005 无文件冲突，但 T005 实现前测试会编译失败——先写测试骨架，T005 完成后跑通）
- T007/T008/T009 串行（逐步验证）

## Implementation Strategy

**MVP scope**：整个 feature 就是一个 User Story，没有分阶段裁剪空间。T001-T009 全部完成 = Lesson 25 交付。

**增量验证**：
1. T001-T003 完成后：`mvn test -pl ibios-core -Dtest=ProfileLoaderTest` 绿 → ScheduleConfig 改造无回归
2. T004-T005 完成后：`mvn test -pl ibios-core -Dtest=AgentSchedulerTest` 绿 → 四个关键回归点守住
3. T006-T008 完成后：`mvn clean verify` 全绿 → 全模块无回归

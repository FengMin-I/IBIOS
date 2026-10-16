# Implementation Plan: AgentScheduler 定时触发

## Metadata

- **Feature**: AgentScheduler 定时触发（Lesson 25）
- **Spec**: `specs/008-agent-scheduler/spec.md`
- **Date**: 2026-10-15
- **Status**: approved

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba（动手前先跑 mvn dependency:tree 确认锁定 BOM 里目标依赖存在）、SQLite + Spring Data JPA。凭证走环境变量占位，不落明文。SQLite 用手工建表脚本，不依赖 hibernate.ddl-auto=update。

定时机制使用 Spring 自带的 `ThreadPoolTaskScheduler` + `CronTrigger`（spring-boot-starter 已有依赖，无需新增）。

---

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `AgentScheduler` | ibios-core |
| `Profile.ScheduleConfig`（补 id 字段） | ibios-core |
| `AgentSchedulerTest` | ibios-core (test) |

---

## 3. Design Decisions

### Decision 1: ScheduleConfig 补 id 字段

当前 `Profile.ScheduleConfig` 只有 `(cron, zone, message)` 三个字段。课件要求 `(id, cron, zone, message)` 四字段。id 用于：
- 防重叠锁的 key（`ConcurrentMap<String, Lock>`）
- 日志标识（`"scheduled task {} failed"` 里的占位）

**改造方式**：修改 `Profile.ScheduleConfig` record，新增 `String id` 作为第一个字段。同步修改 `ProfileLoader` 解析 frontmatter 时读取 id。

### Decision 2: 不用 @Scheduled，用 TaskScheduler 动态注册

`@Scheduled` 的 cron 是编译期常量，改触发时间要改代码重新编译，不符合"配置即 Agent"原则。改用 `ThreadPoolTaskScheduler.schedule(Runnable, Trigger)` 动态注册，cron + 时区一起传给 `CronTrigger`。

### Decision 3: 进程内 ReentrantLock 防重叠

核心阶段单实例部署，每个任务 id 一把 `ReentrantLock`，放 `ConcurrentHashMap<String, Lock>` 管理。`tryLock()` 拿不到就跳过本次触发，不排队、不并行。不用分布式锁。

### Decision 4: 会话身份固定 ("scheduler", "scheduler", profileName)

钟推也要落 Session。channel 和 user 都固定为 `"scheduler"`，profile 取当前 Agent 名称。同一 Profile 的历次定时触发复用同一 Session，对话历史自然累积、靠 `max_history_turns` 截断兜底。

### Decision 5: ThreadPoolTaskScheduler 不违反原则六

原则六禁止"自建线程池"。Spring 的 `ThreadPoolTaskScheduler` 是框架提供的标准调度器（跟 `@EnableScheduling` 配套），不是自己 `new Thread()` 或 `Executors.newXxx()`，符合原则六精神。

---

## 4. Test Strategy

测试策略按课件"验收 harness"执行：`AgentSchedulerTest`（覆盖 cron+时区注册、重叠跳过、失败不崩+锁释放、会话身份约定），单测默认跑、集成冒烟打 `@Tag("integration")` CI 跳过；实现完成的定义是 `mvn clean verify` 全绿。

测试诀窍：**别真等时间**。`runOnce` 拆成包级可见方法，直接调它测全部行为逻辑；cron 触发本身是 Spring 的事，只验"注册参数传对了"（ArgumentCaptor 抓 `CronTrigger`）。

---

## 5. Syntax Restrictions

避开 P3C/ASM 解析不了的 Java 18+ 语法形态（如增强 switch 的 `default ->` 写法），静态检查是构建门禁。

---

## 6. Constitution Check

- [x] Principle 1: JDK 21 + Spring Boot 3.x 单体
- [x] Principle 2: 五大核心能力优先（定时是触发源扩展）
- [x] Principle 3: 自实现 ReAct 循环（AgentScheduler 只调 AgentService）
- [x] Principle 4: Spring AI 只用协议转换（不影响）
- [x] Principle 5: Plugin Tool 三层访问（不影响）
- [x] Principle 6: 同步 + 虚拟线程（ThreadPoolTaskScheduler 是 Spring 标准组件）
- [x] Principle 7: Sandbox 接口优先（定时触发的 Tool 调用仍过 Sandbox）
- [x] Principle 8: SQLite + 审计 Day One（审计走已有路径）
- [x] Principle 9: 显式 Provider 映射（不影响）
- [x] Principle 10: 全局不变量

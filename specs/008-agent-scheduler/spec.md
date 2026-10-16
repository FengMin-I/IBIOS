# Feature Specification: AgentScheduler 定时触发

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: AgentScheduler 定时触发（第三种触发源——钟推）
- **Lesson**: 25
- **Author**: IBIOS Dev
- **Date**: 2026-10-15
- **Status**: draft

---

## 1. Background & Value

Agent 已具备 CLI 人推、Web Service 人推两种触发方式，但大量实用场景（每日天气推送、每日科技日报、定时巡检告警）需要"到点自己干活"。定时任务是第三种触发源——**钟推**：到点了系统自动拼一条消息，喂给跟人推完全一样的处理入口。核心引擎（ReActLoop、Tool、Provider）零改动，只需一个薄薄的调度层把"Profile 里配的定时规则"接到调度能力上。

---

## 2. User Scenarios

### Scenario 1: 配置驱动的定时触发

运维在 Agent 配置（AGENT.md frontmatter）里声明定时规则（cron 表达式 + 时区 + 要发给 Agent 的消息内容），系统启动后自动注册所有定时任务，到点后 Agent 自动发起一轮完整的 ReAct 循环——跟人手动触发走完全一样的链路。

### Scenario 2: 重叠执行防护

一个定时任务跑的时间比调度间隔长（例如每分钟触发一次，但上一次 ReAct 循环还没跑完），本次触发直接跳过、不排队、不并行跑两份，保证同一任务在任意时刻最多只有一个实例在跑。

### Scenario 3: 失败隔离与审计

一次定时任务跑挂了（例如 LLM 调用超时、工具执行异常），调度器本身不崩溃、不影响其他定时任务的下一次触发。失败的这次调用依然走完整的审计路径（LLM 调用记录、工具执行记录），跟人推触发的失败没有区别。

---

## 3. Functional Requirements

- **FR1**: 系统启动时扫描所有 Agent Profile 的定时配置，逐条动态注册到调度器（不使用编译期写死的静态注解）。
- **FR2**: 每条定时配置包含四个字段：任务标识（id）、cron 表达式、时区、触发时要发给 Agent 的消息内容。
- **FR3**: 每个定时任务按任务标识维度独立防重叠——上一次还没跑完，下一次触发到了就跳过，不排队、不并行。
- **FR4**: 定时触发的会话身份固定为 channel="scheduler"、user="scheduler"、profile=当前 Agent 名称，同一 Agent 的历次定时触发复用同一个会话，对话历史自然累积。
- **FR5**: 定时触发最终调用的是跟人推/Web 推完全一样的统一处理入口，后续处理链路零差异。
- **FR6**: 单次定时任务执行失败只记日志不外抛，不影响调度器和其他任务；无论成功失败，防重叠锁必须被释放，不能产生死锁。
- **FR7**: cron 表达式配合显式声明的时区解析执行，不使用服务器默认时区。
- **FR8**: 定时触发的审计路径与人推完全一致，不需要为定时场景单独开审计逻辑。

---

## 4. Out of Scope (Boundary)

- 多实例部署下的分布式协调（选主、分布式锁、租约）——核心阶段单实例，扩展阶段解决
- 失败自动重试、失败次数告警——核心阶段做到"失败不崩、留痕可查"
- 定时任务状态持久化到数据库（任务状态表、执行历史表）——第 28 节补齐
- 运行时通过管理接口增删改 cron 定义——核心阶段只能改配置重启生效

---

## 5. Acceptance Criteria

### Automated (Harness)

测试由 `AgentSchedulerTest` 一个类覆盖四个关键回归点，`mvn test` 全绿即通过：

| 测试点 | 守住的坑 |
|--------|----------|
| 注册时 CronTrigger 带上了配置的 cron 和时区（ArgumentCaptor 抓注册参数） | 时区：不让服务器时区替用户做主 |
| 锁被占时本次触发直接跳过、不排队（verify 处理入口未被调用） | 重叠执行 |
| 处理入口抛异常时不外抛、锁在 finally 里被释放（再触发一次证明锁放了） | 失败隔离 + 不留死锁 |
| 会话三元组固定 ("scheduler", "scheduler", profileName)，两次触发拿到同一 Session | 会话身份约定 |

### Manual

- 真实到点触发一次：schedules 设成"每分钟"，到点看到 Agent 自动发起对话、llm_calls/tool_invocations 有账
- 改 cron 表达式不用重新编译、重启后按新时间跑
- 端到端预演：完整走一遍"到点自动触发 → 跑完 ReAct 循环 → 留下审计记录"

---

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 16**: ProfileRegistry（Agent 配置注册表）、Profile（含 schedules 字段，需补 id 字段）
- **Lesson 17**: ReActLoop、AgentService（统一处理入口）、ProfileContext
- **Lesson 18**: SessionManager（会话管理，getOrCreate 方法）

### External Dependencies

- Spring TaskScheduler + CronTrigger（spring-boot-starter 已有依赖）

### Assumptions

- 核心阶段单实例部署，进程内锁足够
- Profile 的 schedules 字段在 AGENT.md frontmatter 里声明，启动时加载
- ScheduleConfig 需新增 id 字段（当前只有 cron/zone/message），id 用于锁维度和日志标识

---

## Constitution Check

- [x] Principle 1: JDK 21 + Spring Boot 3.x 单体
- [x] Principle 2: 五大核心能力优先（定时是触发源扩展，不新增核心能力）
- [x] Principle 3: 自实现 ReAct 循环（AgentScheduler 只调 AgentService，不碰 ReActLoop 内部）
- [x] Principle 4: Spring AI 只用协议转换，autoExecuteTools 禁用（定时不影响此约束）
- [x] Principle 5: Plugin Tool 三层访问（定时触发走的 Tool 链路不变）
- [x] Principle 6: 同步 + 虚拟线程（TaskScheduler 用 Spring 自带的，不自建线程池——但课件骨架用了 ThreadPoolTaskScheduler，需确认是否违反原则六）
- [x] Principle 7: Sandbox 接口优先（定时触发走的 Tool 都过 Sandbox）
- [x] Principle 8: SQLite + 审计 Day One（定时触发的审计走已有路径）
- [x] Principle 9: 显式 Provider 映射（不影响）
- [x] Principle 10: 全局不变量（审计、无明文 key、session_id 约定、无 Reactor）

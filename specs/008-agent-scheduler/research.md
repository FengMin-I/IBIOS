# Research: AgentScheduler 定时触发

## R1: Spring TaskScheduler vs @Scheduled

- **Decision**: 使用 `ThreadPoolTaskScheduler` + `CronTrigger` 动态注册
- **Rationale**: `@Scheduled` 的 cron 是编译期注解常量，无法从 Profile 配置动态生成。`TaskScheduler.schedule(Runnable, Trigger)` 支持运行时注册，cron + 时区一起传入
- **Alternatives considered**: `@Scheduled` + SpEL（仍受限于 Bean 初始化时解析，无法按 Profile 动态增减）

## R2: CronTrigger 时区支持

- **Decision**: `new CronTrigger(expression, zoneId)` 构造器直接传 ZoneId
- **Rationale**: Spring 的 CronTrigger 支持 `TimeZone` 参数（Spring 6.x 接受 `ZoneId` 通过 `TimeZone.getTimeZone()` 转换）。从 ScheduleConfig.zone（如 "Asia/Shanghai"）解析 ZoneId
- **Alternatives considered**: 全局默认时区（违反"不让服务器时区替用户做主"）

## R3: 防重叠锁粒度

- **Decision**: 按 ScheduleConfig.id 维度一把 ReentrantLock
- **Rationale**: 同一任务不重叠，不同任务互不影响。ConcurrentHashMap.computeIfAbsent 保证线程安全
- **Alternatives considered**: 全局单锁（太粗，不同任务会互相阻塞）；分布式锁（核心阶段单实例不需要）

## R4: Profile.ScheduleConfig 补 id 字段的向后兼容

- **Decision**: id 作为第一个字段加入 record。ProfileLoader 解析 frontmatter 时从 YAML map 读取
- **Rationale**: record 的构造参数顺序变了，所有构造 ScheduleConfig 的地方都需要同步改。当前代码里只有 ProfileLoader 和测试构造它
- **Alternatives considered**: 用 cron 表达式做隐式 id（不够稳定，同一 Agent 可能有多条相同 cron 的任务）

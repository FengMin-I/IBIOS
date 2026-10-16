# Implementation Plan: 插件化 Agent——一个目录定义一个会自己跑的 Agent

> Created by `/speckit-plan`. Fill all sections before proceeding to tasks.

## Metadata

- **Feature**: 插件化 Agent——一个目录定义一个会自己跑的 Agent
- **Spec**: `specs/010-plugin-agent/spec.md`
- **Date**: 2026-10-16
- **Status**: draft

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists
in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data
JPA. Credentials via environment variable placeholders, no plaintext. SQLite
uses manual schema scripts, not `hibernate.ddl-auto=update`.

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `AgentLoader`（解析 AGENT.md → frontmatter + 正文 + 资源路径） | `ibios-core` (`com.ibios.core.agent`) |
| `deriveProfile(Path agentDir) → Profile`（AgentLoader 方法） | `ibios-core` (`com.ibios.core.agent`) |
| `Profile` record 新增 `agentDir` 字段（`java.nio.file.Path`，手写 Profile 为 null） | `ibios-core` (`com.ibios.core.profile`) |
| `ProfileRegistry` 补 `remove(String)` / `exists(String)` | `ibios-core` (`com.ibios.core.profile`) |
| `AgentScheduler.registerProfile(Profile)` + `scheduledTasks` 句柄表 | `ibios-core` (`com.ibios.core.react`) |
| `ContextLoader.loadAgentBody(Profile)` | `ibios-core` (`com.ibios.core.react`) |
| `PromptBuilder` 插入 agent body | `ibios-core` (`com.ibios.core.react`) |
| 启动扫描逻辑（装配层） | `ibios-boot` (`com.ibios.IbiosConfiguration` 或新 `AgentScanner` Bean) |
| 示例 Agent 目录 `.ibios/agents/daily-reconcile/` | 项目根目录 |

## 3. Design Decisions

### Decision 1: Profile record 新增 agentDir 字段

**选择**：`Profile` record 新增 `java.nio.file.Path agentDir` 字段，手写 Profile YAML 来源时为 null，Agent 目录派生时为目录路径。

**理由**：底座一切吃 Profile。把 agentDir 带在 Profile 上，ContextLoader 就能在 buildSystemPrompt 时判断"这个 Profile 来自 Agent 目录"并读 AGENT.md 正文。不需要额外的 ThreadLocal 或全局 Map。

**替代方案**：用 `Map<String, Path> agentDirByProfileName` 放在 AgentLoader 里。缺点：ContextLoader 需要额外注入这个 Map，且生命周期管理复杂。

### Decision 2: AgentLoader 放在 ibios-core/profile 还是 ibios-core/agent

**选择**：新建 `com.ibios.core.agent` 包，放 `AgentLoader` 类。

**理由**：Agent 目录扫描和 Profile 派生是独立关注点，与 Profile YAML 加载（`ProfileLoader`）平行但不同源。新包避免 `profile` 包膨胀。`AgentLoader` 只依赖 SnakeYAML（已在 ibios-core），不引入 Spring。

### Decision 3: ContextLoader.loadAgentBody 的注入位置

**选择**：`PromptBuilder.buildSystemPrompt` 在 `identity.prompt` 之后、`bootstrap` 之前调用 `contextLoader.loadAgentBody(profile)`。当 `profile.agentDir()` 非空时，读 `{agentDir}/AGENT.md`、去 frontmatter 取正文。

**理由**：Agent 正文就是这个 Agent 的"人格 + 干什么"，与 identity.prompt 同层。identity.prompt 来自 frontmatter 的 `identity.prompt` 字段（短人格描述），正文是完整的任务指令。两者都进 system prompt，正文在人格之后。

### Decision 4: AgentScheduler.registerProfile 与 scheduledTasks 句柄表

**选择**：从 `registerAll` 循环体抽出 `registerProfile(Profile)` 方法。新增 `Map<String, ScheduledFuture<?>> scheduledTasks` 字段。`registerProfile` 注册定时后把 `ScheduledFuture` 存入句柄表。

**理由**：第 30 节 DELETE /api/v1/agents/{name} 需要注销定时，句柄表是前提。`registerProfile` 让启动扫描和运行时新增走同一段代码。

### Decision 5: 启动扫描放在装配层

**选择**：在 `ibios-boot` 的 `IbiosConfiguration`（或新建 `AgentScannerConfiguration`）里，`@PostConstruct` 或 `CommandLineRunner` 中调 `AgentLoader.scanAndRegister(agentsDir, registry, scheduler)`。

**理由**：扫描是启动行为，涉及 ProfileRegistry + AgentScheduler 两个 Bean，属于装配层职责。ibios-core 的 AgentLoader 只做解析和派生，不感知 Spring。

## 4. Test Strategy

Tests follow the lesson's acceptance harness:

- **AgentLoaderTest**（ibios-core）：frontmatter/正文拆分、资源路径识别、缺必填字段报错
- **DeriveProfileTest**（ibios-core）：frontmatter → Profile 字段映射、schedules 原样带入、agentDir 设置
- **AgentScanRegisterTest**（ibios-core）：N 个目录 → Registry 出现 N 个、schedules 进 Scheduler
- **ProfileRegistryRuntimeTest**（ibios-core）：register/remove/exists 运行时可变性、非法配置异常一致性
- **AgentSchedulerRegisterTest**（ibios-core）：registerProfile 后 scheduledTasks 有句柄、cron/时区正确
- **ProgressiveDisclosureTest**（ibios-core）：正文进 system prompt、参考/脚本不预载

单测默认跑、集成冒烟打 `@Tag("integration")` CI 跳过；实现完成的定义是 `mvn clean verify` 全绿。

## 5. Syntax Restrictions

Avoid Java 18+ syntax that P3C/ASM cannot parse (e.g., enhanced switch
`default ->` form). Static analysis is a build gate.

传统 switch-case-break，不用增强 switch。测试方法名英文，`@DisplayName` 保留中文。

## 6. Constitution Check

- [x] All principles from spec verified
- [x] Module placement follows §10 dependency direction（AgentLoader → ibios-core，不引入 Spring）
- [x] No reactive imports
- [x] No Spring AI auto-execution
- [x] Audit tables updated if applicable（不涉及新表）
- [x] Sandbox enforcement noted for IO paths（脚本走既有 shell 工具的 Sandbox 校验）

# Tasks: 插件化 Agent——一个目录定义一个会自己跑的 Agent

> Created by `/speckit-tasks`. Dependency-ordered, each task testable.

## Metadata

- **Feature**: 插件化 Agent——一个目录定义一个会自己跑的 Agent
- **Plan**: `specs/010-plugin-agent/plan.md`
- **Date**: 2026-10-16
- **Status**: pending

---

## Phase 1: 基础改造（Profile + ProfileRegistry + 示例目录）

### T001 Profile record 新增 agentDir 字段 `[I]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/main/java/com/ibios/core/profile/Profile.java`
- **Details**: 在 Profile record 末尾新增 `java.nio.file.Path agentDir` 字段。所有现有调用方（ProfileLoader.convertToProfile、测试等）补 `null`。compact constructor 不变。
- **Acceptance**: 编译通过，现有测试不红

### T002 ProfileRegistry 补 remove/exists `[I]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/main/java/com/ibios/core/profile/ProfileRegistry.java`
- **Details**: 新增 `void remove(String name)` 和 `boolean exists(String name)` 方法。
- **Acceptance**: 编译通过

### T003 ProfileRegistryRuntimeTest `[T]`

- **Depends on**: T002
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/profile/ProfileRegistryRuntimeTest.java`
- **Details**: register 后 get 可见；remove 后 get 返回 null、exists 返回 false；exists 对已注册返回 true；remove 不存在的 key 不报错。测试方法名英文，@DisplayName 中文。
- **Acceptance**: `mvn test -pl ibios-core -Dtest=ProfileRegistryRuntimeTest` 全绿

### T004 示例 Agent 目录 daily-reconcile `[C]`

- **Depends on**: —
- **Deliverables**:
  - `.ibios/agents/daily-reconcile/AGENT.md`
  - `.ibios/agents/daily-reconcile/scripts/reconcile.py`
  - `.ibios/agents/daily-reconcile/skills/report-format.md`
  - `.ibios/agents/daily-reconcile/REFERENCE.md`
- **Details**: 按课件 §1.3-1.4 全文产出四个文件。AGENT.md 的 frontmatter 包含 name/description/identity/provider/tools/notify_channels/schedules。
- **Acceptance**: 四个文件存在且内容与课件一致

---

## Phase 2: AgentLoader 核心

### T005 AgentLoaderTest `[T]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/agent/AgentLoaderTest.java`
- **Details**: 测试 `load(Path)` 方法：正确拆出 frontmatter 与正文；认出 scripts/skills/REFERENCE.md 路径；AGENT.md 不存在抛 IllegalArgumentException；缺 name 字段抛异常；缺 provider 字段抛异常。用 @TempDir 创建临时 Agent 目录。
- **Acceptance**: 测试编译通过（先写测试，实现前会红）

### T006 DeriveProfileTest `[T]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/agent/DeriveProfileTest.java`
- **Details**: 测试 `deriveProfile(Path)` 方法：frontmatter 各字段正确映射到 Profile；agentDir 设为目录绝对路径；schedules 原样带入 Profile；name 取 frontmatter 的 name 字段。
- **Acceptance**: 测试编译通过

### T007 AgentLoader 实现 `[I]`

- **Depends on**: T001, T005, T006
- **Deliverables**:
  - `ibios-core/src/main/java/com/ibios/core/agent/AgentLoader.java`
  - `ibios-core/src/main/java/com/ibios/core/agent/AgentDefinition.java`
- **Details**:
  - `AgentDefinition` record：name, frontmatter(Map), body(String), agentDir(Path), scriptsDir(Path), skillsDir(Path), referenceFile(Path)
  - `AgentLoader.load(Path agentDir)`：读 AGENT.md，按 `---` 拆 frontmatter/正文，检查资源目录
  - `AgentLoader.deriveProfile(Path agentDir)`：load + 映射为 Profile，设 agentDir
  - frontmatter 解析复用 SnakeYAML + ProfileLoader 的 resolveEnvVars
  - 缺 name/provider 抛 IllegalArgumentException
- **Acceptance**: `mvn test -pl ibios-core -Dtest=AgentLoaderTest,DeriveProfileTest` 全绿

---

## Phase 3: AgentScheduler 改造

### T008 AgentSchedulerRegisterTest `[T]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/react/AgentSchedulerRegisterTest.java`
- **Details**: 测试 `registerProfile(Profile)` 方法：注册后 scheduledTasks 有句柄；cron/时区来自 Profile.schedules。需要 mock TaskScheduler 和 ScheduledTaskStore。
- **Acceptance**: 测试编译通过

### T009 AgentScheduler.registerProfile + scheduledTasks `[I]`

- **Depends on**: T008
- **Deliverables**: `ibios-core/src/main/java/com/ibios/core/react/AgentScheduler.java`
- **Details**:
  - 新增 `Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>()`
  - 从 `registerAll` 循环体抽出 `registerProfile(Profile)` 方法
  - `registerAll` 改为遍历 `profileRegistry.getAll()` 调 `registerProfile`
  - `registerProfile` 注册定时后把 ScheduledFuture 存入 scheduledTasks
  - 新增 `scheduledTasks()` getter 返回不可变视图
- **Acceptance**: `mvn test -pl ibios-core -Dtest=AgentSchedulerRegisterTest,AgentSchedulerTest` 全绿

---

## Phase 4: 扫描注册 + 渐进式披露

### T010 AgentScanRegisterTest `[T]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/agent/AgentScanRegisterTest.java`
- **Details**: 测试 `scanAndRegister`：@TempDir 下放 N 个 Agent 目录 → ProfileRegistry 出现 N 个；带 schedules 的 Profile 调了 AgentScheduler.registerProfile；provider 不在 validProviderNames 中 → 报错；tools 引用未注册工具 → LOG.warning 但不阻断。
- **Acceptance**: 测试编译通过

### T011 ProgressiveDisclosureTest `[T]`

- **Depends on**: —
- **Deliverables**: `ibios-core/src/test/java/com/ibios/core/react/ProgressiveDisclosureTest.java`
- **Details**: 测试：agentDir 非空的 Profile，ContextLoader.loadAgentBody 返回 AGENT.md 正文（去 frontmatter）；agentDir 为 null 时返回空字符串；正文里的 skills/REFERENCE.md 内容不预载（不进 system prompt）。
- **Acceptance**: 测试编译通过

### T012 ContextLoader.loadAgentBody + PromptBuilder 集成 `[I]`

- **Depends on**: T001, T011
- **Deliverables**:
  - `ibios-core/src/main/java/com/ibios/core/react/ContextLoader.java`
  - `ibios-core/src/main/java/com/ibios/core/react/PromptBuilder.java`
- **Details**:
  - ContextLoader 新增 `loadAgentBody(Profile)`：agentDir 非空时读 `{agentDir}/AGENT.md`，去 frontmatter 取正文
  - PromptBuilder.buildSystemPrompt 在 identity.prompt 之后、bootstrap 之前调用 loadAgentBody
- **Acceptance**: `mvn test -pl ibios-core -Dtest=ProgressiveDisclosureTest,ContextLoaderTest` 全绿

### T013 AgentLoader.scanAndRegister 实现 `[I]`

- **Depends on**: T007, T009, T010
- **Deliverables**: `ibios-core/src/main/java/com/ibios/core/agent/AgentLoader.java`（补充 scanAndRegister 方法）
- **Details**:
  - `scanAndRegister(Path agentsDir, ProfileRegistry registry, AgentScheduler scheduler, Set<String> validProviderNames, Set<String> registeredToolNames)`
  - 遍历 agentsDir 子目录 → deriveProfile → 校验 provider → 校验 tools（warning） → registry.register → scheduler.registerProfile
- **Acceptance**: `mvn test -pl ibios-core -Dtest=AgentScanRegisterTest` 全绿

---

## Phase 5: 装配层集成

### T014 装配层启动扫描 `[I]`

- **Depends on**: T013
- **Deliverables**: `ibios-boot/src/main/java/com/ibios/IbiosConfiguration.java`（或新建 AgentScannerConfiguration）
- **Details**: 在装配层注入 AgentLoader Bean，启动时调 scanAndRegister 扫描 `.ibios/agents/`。需要传入 ProfileRegistry、AgentScheduler、validProviderNames、registeredToolNames。
- **Acceptance**: 启动时 `.ibios/agents/daily-reconcile/` 被扫描并注册到 ProfileRegistry

---

## Phase 6: 验证与收尾

### T015 全量构建验证 `[V]`

- **Depends on**: T014
- **Deliverables**: —
- **Details**: `mvn clean verify` 全绿（含 P3C/SpotBugs/FindSecBugs/PMD）。红了当场修。
- **Acceptance**: BUILD SUCCESS，所有测试通过

### T016 存在性核对 `[V]`

- **Depends on**: T015
- **Deliverables**: —
- **Details**: 逐项核对课件"本节交付物"：AgentLoader、deriveProfile、ProfileRegistry.remove/exists、AgentScheduler.registerProfile/scheduledTasks、ContextLoader.loadAgentBody、daily-reconcile/ 四文件、6 个测试类。
- **Acceptance**: 所有交付物存在且非空

---

## Deliverables Checklist

对照课件"本节交付物"：

### 代码
- [ ] `AgentLoader`（ibios-core/com.ibios.core.agent）
- [ ] `AgentDefinition`（ibios-core/com.ibios.core.agent）
- [ ] `deriveProfile(agentDir) → Profile`（AgentLoader 方法）
- [ ] `scanAndRegister`（AgentLoader 方法）
- [ ] `Profile` record 新增 `agentDir` 字段
- [ ] `ProfileRegistry.remove/exists`
- [ ] `AgentScheduler.registerProfile` + `scheduledTasks` 句柄表
- [ ] `ContextLoader.loadAgentBody`
- [ ] `PromptBuilder` 集成 agent body
- [ ] 装配层启动扫描

### 测试
- [ ] `AgentLoaderTest`
- [ ] `DeriveProfileTest`
- [ ] `AgentScanRegisterTest`
- [ ] `ProfileRegistryRuntimeTest`
- [ ] `AgentSchedulerRegisterTest`
- [ ] `ProgressiveDisclosureTest`

### 文件
- [ ] `.ibios/agents/daily-reconcile/AGENT.md`
- [ ] `.ibios/agents/daily-reconcile/scripts/reconcile.py`
- [ ] `.ibios/agents/daily-reconcile/skills/report-format.md`
- [ ] `.ibios/agents/daily-reconcile/REFERENCE.md`

---

## 任务比对：课件"本节交付物" ↔ 任务清单

| 课件交付物 | 对应任务 | 状态 |
|-----------|---------|------|
| AgentLoader | T005, T007 | ✓ |
| deriveProfile | T006, T007 | ✓ |
| 启动扫描 | T013, T014 | ✓ |
| ProfileRegistry.register/remove/exists | T002, T003 | ✓ |
| AgentScheduler.registerProfile + scheduledTasks | T008, T009 | ✓ |
| ContextLoader 注入 Agent 正文 | T011, T012 | ✓ |
| AgentLoaderTest | T005 | ✓ |
| DeriveProfileTest | T006 | ✓ |
| AgentScanRegisterTest | T010 | ✓ |
| ProfileRegistryRuntimeTest | T003 | ✓ |
| AgentSchedulerRegisterTest | T008 | ✓ |
| ProgressiveDisclosureTest | T011 | ✓ |
| daily-reconcile/ 四文件 | T004 | ✓ |
| 校验：缺必填字段报错 | T005, T007 | ✓ |
| 校验：tools 引用未注册 → 告警 | T010, T013 | ✓ |

**结论**：齐。课件交付物逐项有对应任务，无遗漏、无多余。

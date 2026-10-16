# Data Model: 插件化 Agent

## 变更概述

本节不新增数据库表。变更集中在内存值对象和运行时注册机制。

## Profile Record 变更

### 新增字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `agentDir` | `java.nio.file.Path` | `null` | Agent 目录路径。手写 Profile YAML 来源时为 null；Agent 目录派生时为 `.ibios/agents/<name>/` 的绝对路径 |

### 既有字段映射（AGENT.md frontmatter → Profile）

| AGENT.md frontmatter 字段 | Profile 字段 | 说明 |
|--------------------------|-------------|------|
| `name` | `name` | 唯一标识 = 目录名 |
| `description` | `description` | Agent 描述 |
| `identity.agent_name` | `identity.agentName` | Agent 人格名 |
| `identity.prompt` | `identity.prompt` | 短人格描述 |
| `provider.name` | `provider.name` | Provider 名称 |
| `provider.model`（顶层） | `provider.model` | 模型名 |
| `provider.temperature` | `provider.temperature` | 温度 |
| `tools` | `tools` | 工具列表 |
| `notify_channels` | `notifyChannels` | 通知渠道 |
| `schedules` | `schedules` | 定时配置 |
| `settings` | `settings` | 其他设置 |
| `max_iterations` | `maxIterations` | 最大迭代数 |
| `max_history_turns` | `maxHistoryTurns` | 最大历史轮数 |

## ProfileRegistry 变更

### 新增方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `remove` | `void remove(String name)` | 按名字移除 Profile |
| `exists` | `boolean exists(String name)` | 判断 Profile 是否已注册 |

## AgentScheduler 变更

### 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `scheduledTasks` | `Map<String, ScheduledFuture<?>>` | 定时任务句柄表，key = schedule id |

### 新增方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `registerProfile` | `void registerProfile(Profile profile)` | 注册一个 Profile 的所有定时任务 |
| `unregisterProfile` | `void unregisterProfile(String profileName)` | 注销一个 Profile 的所有定时任务（第 30 节用，本节可先留空实现） |

## AgentLoader（新类）

### 方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `load` | `AgentDefinition load(Path agentDir)` | 解析 AGENT.md，返回 AgentDefinition |
| `deriveProfile` | `Profile deriveProfile(Path agentDir)` | load + 映射为 Profile |
| `scanAndRegister` | `void scanAndRegister(Path agentsDir, ProfileRegistry registry, AgentScheduler scheduler, Set<String> validProviderNames, Set<String> registeredToolNames)` | 扫描目录、派生、注册 |

### AgentDefinition（内部值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | Agent 名 |
| `frontmatter` | `Map<String, Object>` | frontmatter 原始数据 |
| `body` | `String` | AGENT.md 正文（去 frontmatter） |
| `agentDir` | `Path` | Agent 目录绝对路径 |
| `scriptsDir` | `Path` | scripts/ 目录（可能不存在） |
| `skillsDir` | `Path` | skills/ 目录（可能不存在） |
| `referenceFile` | `Path` | REFERENCE.md（可能不存在） |

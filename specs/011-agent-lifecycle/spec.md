# 第 30 节需求：动态管理 Agent——API 增删改查 + 实时监听 + 一句话生成 + 工作区浏览

## 概述

第 29 节立住了"一个 Agent = 一个目录"和运行时注册能力（`ProfileRegistry.register/remove`、`AgentScheduler.registerProfile`），但入口还停在"登录服务器往目录里放东西、重启才生效"。本节把最后一块拼上：让 Agent 目录**实时生效**（丢进去即上线、免重启），包成 REST 端点，加上**一句话自动生成 Agent 文件**和**工作区文件浏览器**——两条录入路径（API 创建 / 手工丢目录）殊途同归，底层走同一段 `register(agentDir)`。

同时增补三块能力：每个 Agent 专属记忆（per-agent `MEMORY.md`）、一个 Agent 一个固定会话（上下文累积）、工作区文件可编辑保存。

## 用户场景

1. **运营在管理台一句话造 Agent**：填 name + description → 系统脚手架出完整目录（`AGENT.md` + `scripts/` + `skills/` + `REFERENCE.md`）→ 注册上线，不重启。
2. **运营对已有 Agent 说一句话重新生成文件**：描述想要的变更 → LLM 生成 `AGENT.md` 草稿 → 预览可改 → 保存即生效。
3. **运维直接往 `.ibios/agents/` 丢目录**：scp / git pull / 编辑器创建 Agent 目录 → `WorkspaceWatcher` 实时拾取、自动注册，与 API 创建行为完全一致。

## 功能需求

### Agent CRUD

FR1. `POST /api/v1/agents`：创建 Agent。请求体 `{name, description}` → 脚手架完整目录（`AGENT.md` + `scripts/` + `skills/` + `REFERENCE.md`，模板内容）→ `deriveProfile` → `ProfileRegistry.register` → 有 `schedules` 则 `AgentScheduler.registerProfile`。失败回滚（已写目录删回）。
FR2. `GET /api/v1/agents`：列出所有已注册 Agent（含来自文件扫描和 API 创建的）。
FR3. `GET /api/v1/agents/{name}`：查单个 Agent 详情（AgentView：name/description/provider/model/tools/schedules/agentDir）。
FR4. `PUT /api/v1/agents/{name}`：覆写 `AGENT.md`。`schedules` 变则先注销旧句柄、再注册新的。
FR5. `DELETE /api/v1/agents/{name}`：注销定时 → 移出索引 → 目录归档 `.ibios/archive/`（不物理删）。
FR6. `POST /api/v1/agents/{name}/invoke`：26 节已有的无状态调用，不变。

### 一句话生成与文件管理

FR7. `POST /api/v1/agents/{name}/generate-files`：对已存在 Agent，一次 LLM 调用产出 `AGENT.md` → `AgentLoader.parse` 校验能解析 → 返回 `{相对路径 → 内容}` 给前端**预览可改**，**不落盘、不注册**。provider 若该 Agent 已存在则沿用。LLM 多吐的 ``` 代码围栏剥掉。
FR8. `POST /api/v1/agents/{name}/files`：保存一组文件——先校验 `AGENT.md` 可解析（非法 → 400）→ `agentStore.writeAll` → 覆写后重注册（`schedules` 变更先注销旧的）。

### Per-Agent 记忆

FR9. 每个 Agent 专属 `MEMORY.md`：`.ibios/agents/<name>/MEMORY.md`。通过 `ToolExecutionContext`（`ThreadLocal<String> agentName`）实现——`ToolExecutor` 执行工具前置入 `profile.name()`、执行后清除。`save_memory`/`recall_memory` 据此落到本 Agent 的 `MEMORY.md`。
FR10. `GET /api/v1/agents/{name}/memory`：读这个 Agent 的专属记忆。**移除**全局 `GET /api/v1/memory`。

### Per-Agent 固定会话

FR11. 每个 Agent 在管理台恰好一条固定会话：`channel="admin"` + `user="console"` + `profile=<Agent 名>`，`getOrCreate` 幂等。
FR12. `GET /api/v1/agents/{name}/session`：返回这条会话的 SessionView（sessionId + profileName + 最近 ≤100 条消息）。
FR13. `POST /api/v1/agents/{name}/session/messages`：往固定会话发消息、触发 ReAct（同 invoke 入口，但累积上下文）。

### 工作区文件浏览器

FR14. `GET /api/v1/workspace/tree`：返回 `.ibios/agents/` 与 `.ibios/archive/` 的目录树。
FR15. `GET /api/v1/workspace/file?path=...`：读文件文本内容。**必做防目录穿越**：`normalize()` 后 `startsWith(root)` 校验，越界 400。
FR16. `POST /api/v1/workspace/file`：写文件（防穿越）。编辑 `agents/<name>/AGENT.md` 时走 `AgentLifecycleService.update`（写 + 校验 + 重注册）。

### 实时目录监听

FR17. `WorkspaceWatcher`：启动全量扫 + JDK `WatchService` 实时监听 `.ibios/agents/` 变更。新增/改 → `register(agentDir)`；删除 → 注销。单个坏目录不拖垮监听。是后台守护线程（跟 `AgentScheduler` 调度线程同类，不违反宪法七）。

### 错误处理

FR18. 错误码沿用 26 节口径：name 冲突 / 字段非法 / provider 不存在 → 400；查/改/删不存在 → 404。统一 `ApiResponse` 信封。
FR19. 生成用 provider/model 配置：`ibios.author.provider` / `ibios.author.model`。provider 缺省取 `ibios.providers` 第一个；model 留空则生成端点返回 503。

## 明确不做（边界）

- 认证鉴权（内网假设，扩展阶段补 API Key + RBAC）
- Agent 启用/停用状态位
- 创建时 dry-run 试跑
- Agent 版本历史
- 一句话生成的多轮追问式细化
- 带脚本的复杂 Agent 走 multipart/zip 上传（JSON create 只脚手架模板目录，带脚本走手工丢目录）

## 验收标准

可自动化部分由课件"验收 harness"测试套件承载（mvn test 全绿即通过）：

- **AgentLifecycleServiceTest**：create 按序执行；name 冲突在第一步就拒、不写目录；**注册失败时回滚已写目录**；create 与 watcher 走同一段 `register(agentDir)`；delete 按"注销定时 → 移出索引 → 归档"顺序；update 改 schedules 时先 unregister 后 register
- **WorkspaceWatcherTest**：手工丢目录 → 监听触发 register、Agent 出现在 ProfileRegistry；删目录 → 注销；单个坏目录不拖垮监听
- **WorkspaceApiControllerTest**：tree 返回 agents/archive 结构；**`file?path=../../etc/passwd` 目录穿越 → 400**（关键回归）；正常文件返回内容
- **GenerateTest**：generate 产出能被 AgentLoader 解析的 AGENT.md 草稿；只生成不落盘不注册；LLM 产出非法 → 400
- **AgentApiControllerTest**（standalone MockMvc）：端点薄转发；冲突 → 400、不存在 → 404，统一 ApiResponse

关键回归点：编排顺序与回滚、目录穿越防御、两条录入路径同一段注册代码。

## 依赖与假设

- 依赖 Lesson 29：`AgentLoader.deriveProfile`、`ProfileRegistry.register/remove/exists`、`AgentScheduler.registerProfile` + `scheduledTasks` 句柄表
- 依赖 Lesson 26：`AgentApiController`（已有 invoke）、`ApiResponse`、`GlobalExceptionHandler`
- 依赖 Lesson 17：`AgentService`
- 依赖 Lesson 18：`SessionManager`
- 依赖 Lesson 22：`MemoryService`
- 模块落位：`AgentLifecycleService` → ibios-core；`AgentApiController`（扩展）/`WorkspaceApiController` → ibios-web；`WorkspaceWatcher` → ibios-core；`ToolExecutionContext` → ibios-core
- 生成用系统提示词：`docs/prompt/prompt.md`（`AGENT_AUTHOR_PROMPT`）
- 新增配置：`ibios.author.provider` / `ibios.author.model`

## 本节交付物

### 代码

- `AgentApiController`：六个端点（create/get/list/update/delete/invoke）+ generate-files + files + memory + session + session/messages
- `WorkspaceApiController`：tree/file( GET + POST)，防目录穿越
- `AgentLifecycleService`：create（含回滚）/ register(agentDir) / get / list / update / delete / generateFiles / saveFiles
- `WorkspaceWatcher`：启动全量扫 + 实时监听 → 同一段 register(agentDir)
- `AgentScheduler.unregisterProfile`：遍历 schedules、从 scheduledTasks 取句柄 cancel
- `AgentStore`：write / writeAll / archive / delete
- `ToolExecutionContext`：ThreadLocal agentName，ToolExecutor 前置入后清
- `AgentView` / `FileNode` / `SessionView` 与请求体 DTO
- `GenerateRequest` / `CreateAgentRequest` / `UpdateAgentRequest` / `SaveFilesRequest`
- 配置：`ibios.author.provider` / `ibios.author.model`

### 测试（harness）

- `AgentLifecycleServiceTest`
- `WorkspaceWatcherTest`
- `WorkspaceApiControllerTest`
- `GenerateTest`
- `AgentApiControllerTest`

### 目录

- `.ibios/archive/`（删除归档，不物理删）

### 前端

- 管理平台 Agent 列表页（含"新建 Agent"）
- Agent 详情页 5 个 tab：基本信息 / 生成 / 文件（可编辑）/ 会话 / 记忆

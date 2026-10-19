# Tasks: Agent Lifecycle Management (Lesson 30)

## Phase 1: 基础设施

- [ ] T001 实现 `AgentStore`（write / writeAll / archive / delete）in `ibios-core/src/main/java/com/ibios/core/agent/AgentStore.java`
- [ ] T002 实现 `ToolExecutionContext`（ThreadLocal<String> agentName）in `ibios-core/src/main/java/com/ibios/core/agent/ToolExecutionContext.java`
- [ ] T003 实现 `IbiosAuthorProperties`（`ibios.author.provider` / `ibios.author.model`）in `ibios-boot/src/main/java/com/ibios/config/IbiosAuthorProperties.java`
- [ ] T004 实现 `AgentScheduler.unregisterProfile(Profile)` — 遍历 schedules、从 scheduledTasks 取句柄 cancel(false) in `ibios-core/src/main/java/com/ibios/core/react/AgentScheduler.java`
- [ ] T005 创建脚手架模板文件（AGENT.md / REFERENCE.md / scripts/.gitkeep / skills/.gitkeep）in `ibios-core/src/main/resources/agent-templates/`
- [ ] T006 [P] 写 `AgentStoreTest` in `ibios-core/src/test/java/com/ibios/core/agent/AgentStoreTest.java`
- [ ] T007 [P] 写 `ToolExecutionContextTest` in `ibios-core/src/test/java/com/ibios/core/agent/ToolExecutionContextTest.java`
- [ ] T008 [P] 写 `AgentSchedulerUnregisterTest` in `ibios-core/src/test/java/com/ibios/core/react/AgentSchedulerUnregisterTest.java`

## Phase 2: 核心编排

- [ ] T009 实现 `AgentLifecycleService`（create / register / get / list / update / delete / generateFiles / saveFiles）in `ibios-core/src/main/java/com/ibios/core/agent/AgentLifecycleService.java`
- [ ] T010 实现 `WorkspaceWatcher`（启动全量扫 + WatchService 监听 → register(agentDir)）in `ibios-core/src/main/java/com/ibios/core/agent/WorkspaceWatcher.java`
- [ ] T011 织入 `ToolExecutionContext` 到 `ToolExecutor.execute` — 前置入 profile.name()、执行后清除 in `ibios-core/src/main/java/com/ibios/core/react/ToolExecutor.java`
- [ ] T012 改造 `MemoryServiceImpl` 支持 per-agent 路径 — 读 ToolExecutionContext 决定 MEMORY.md 位置 in `ibios-memory/src/main/java/com/ibios/memory/MemoryServiceImpl.java`
- [ ] T013 [P] 写 `AgentLifecycleServiceTest`（harness：create 按序、冲突早拒、注册失败回滚、delete 顺序、update 先 unregister 后 register）in `ibios-core/src/test/java/com/ibios/core/agent/AgentLifecycleServiceTest.java`
- [ ] T014 [P] 写 `WorkspaceWatcherTest`（harness：手工丢目录 → register、删目录 → 注销、坏目录不崩）in `ibios-core/src/test/java/com/ibios/core/agent/WorkspaceWatcherTest.java`
- [ ] T015 [P] 写 `GenerateTest`（harness：产出可解析、不落盘不注册、非法 → 异常）in `ibios-core/src/test/java/com/ibios/core/agent/GenerateTest.java`

## Phase 3: Web 层

- [ ] T016 创建 DTO：`AgentView` / `FileNode` / `SessionView` / `CreateAgentRequest` / `UpdateAgentRequest` / `SaveFilesRequest` / `GenerateRequest` / `SendMessageRequest` in `ibios-web/src/main/java/com/ibios/web/model/`
- [ ] T017 扩展 `AgentApiController`：+create / +list / +get / +update / +delete / +generateFiles / +files / +memory / +session / +sessionMessages in `ibios-web/src/main/java/com/ibios/web/controller/AgentApiController.java`
- [ ] T018 实现 `WorkspaceApiController`（tree / file GET / file POST，防目录穿越）in `ibios-web/src/main/java/com/ibios/web/controller/WorkspaceApiController.java`
- [ ] T019 [P] 写 `AgentApiControllerTest`（harness：薄转发、冲突 400、不存在 404、统一 ApiResponse）in `ibios-web/src/test/java/com/ibios/web/controller/AgentApiControllerTest.java`
- [ ] T020 [P] 写 `WorkspaceApiControllerTest`（harness：tree 结构、**目录穿越 → 400**、正常文件返回）in `ibios-web/src/test/java/com/ibios/web/controller/WorkspaceApiControllerTest.java`

## Phase 4: 配置与集成

- [ ] T021 `application.yaml` 新增 `ibios.author.provider` / `ibios.author.model` 配置 in `ibios-boot/src/main/resources/application.yaml`
- [ ] T022 `IbiosConfiguration` 注册 `AgentLifecycleService` / `AgentStore` / `WorkspaceWatcher` Bean in `ibios-boot/src/main/java/com/ibios/config/IbiosConfiguration.java`
- [ ] T023 改造 `AgentInitializer` → 交给 `WorkspaceWatcher` 统一扫描（移除启动扫描逻辑）in `ibios-boot/src/main/java/com/ibios/config/AgentInitializer.java`
- [ ] T024 创建 `.ibios/archive/` 目录（.gitkeep）
- [ ] T025 创建 `docs/prompt/prompt.md`（AGENT_AUTHOR_PROMPT）

## Phase 5: 前端

- [ ] T026 Agent 列表页（含"新建 Agent"按钮）in `ibios-web/src/main/frontend/src/views/`
- [ ] T027 Agent 详情页 5 tab（基本信息 / 生成 / 文件 / 会话 / 记忆）in `ibios-web/src/main/frontend/src/views/`
- [ ] T028 前端路由与导航更新 in `ibios-web/src/main/frontend/src/router/`

## Phase 6: 验证

- [ ] T029 `mvn clean verify` 全绿
- [ ] T030 前序节全部测试回归绿
- [ ] T031 H4 六条全局不变量逐条自查
- [ ] T032 验收报告

---

## 依赖关系

- Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6
- Phase 1 内部：T001-T005 可并行，T006-T008 依赖对应实现
- Phase 2 内部：T009-T012 顺序执行，T013-T015 依赖 T009/T010
- Phase 3 内部：T016 先于 T017-T018，T019-T020 依赖对应 Controller
- Phase 4：T021-T025 可并行
- Phase 5：T026-T028 顺序执行

## 课件交付物比对

| 课件交付物 | 对应 Task |
|---|---|
| `AgentApiController` 六个端点 + generate-files/files/memory/session/sessionMessages | T017 |
| `WorkspaceApiController`（tree/file GET/file POST） | T018 |
| `AgentLifecycleService` | T009 |
| `WorkspaceWatcher` | T010 |
| `AgentScheduler.unregisterProfile` | T004 |
| `AgentStore` | T001 |
| `ToolExecutionContext` | T002, T011 |
| `AgentView` / `FileNode` / DTO | T016 |
| `AgentLifecycleServiceTest` | T013 |
| `WorkspaceWatcherTest` | T014 |
| `WorkspaceApiControllerTest` | T020 |
| `GenerateTest` | T015 |
| `AgentApiControllerTest` | T019 |
| `.ibios/archive/` | T024 |
| 前端 Agent 管理 + 详情页 | T026-T028 |
| per-agent MEMORY.md | T012 |
| per-agent 固定会话 | T017 (session/sessionMessages 端点) |

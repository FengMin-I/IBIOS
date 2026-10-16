# Tasks: Web Service 与第一版管理平台

> Created by `/speckit-tasks`. Dependency-ordered, each task testable.

## Metadata

- **Feature**: Web Service 与第一版管理平台——IBIOS 的对外 REST API 门面
- **Plan**: `specs/009-web-service-admin/plan.md`
- **Spec**: `specs/009-web-service-admin/spec.md`
- **Lesson**: 26
- **Date**: 2026-10-15
- **Status**: pending

---

## 用户故事映射

| Story | 描述 | 优先级 | 端点 |
|-------|------|--------|------|
| US1 | Session 管理 + Agent 调用（REST API 核心链路） | P1 | POST/GET/DELETE /sessions, POST /sessions/{id}/messages, POST /agents/{name}/invoke |
| US2 | 信息查询（Profile/Tool/Memory 只读查询） | P2 | GET /profiles, GET /tools, GET /memory |
| US3 | 系统状态（health + info） | P3 | GET /health, GET /info |
| US4 | 管理平台前端（Vue 3 + Vite 只读管理台） | P4 | /admin 静态前端 |

---

## Phase 1: 基础设施（异常类 + 模型类 + 配置）

> 所有后续 Controller 的共享依赖。先建异常类和模型类，Controller 才能编译。

- [x] T001 [P] 新建 `SessionNotFoundException` 异常类 in `ibios-web/src/main/java/com/ibios/web/exception/SessionNotFoundException.java`
- [x] T002 [P] 新建 `AgentNotFoundException` 异常类 in `ibios-web/src/main/java/com/ibios/web/exception/AgentNotFoundException.java`
- [x] T003 [P] 新建 `AgentTimeoutException` 异常类 in `ibios-web/src/main/java/com/ibios/web/exception/AgentTimeoutException.java`
- [x] T004 [P] 新建 `InvalidRequestException` 异常类 in `ibios-web/src/main/java/com/ibios/web/exception/InvalidRequestException.java`
- [x] T005 扩展 `GlobalExceptionHandler`：新增 `SessionNotFoundException`→404、`AgentNotFoundException`→404、`AgentTimeoutException`→504、`InvalidRequestException`→400、`IllegalStateException`→503 映射；500 兜底不泄漏 `e.getMessage()` in `ibios-web/src/main/java/com/ibios/web/exception/GlobalExceptionHandler.java`
- [x] T006 [P] 新建请求/响应模型类（`SendMessageRequest`、`MessageResponse`、`SessionResponse`、`InvokeRequest`、`InvokeResponse`、`ProfileSummary`、`ToolSummary`、`MemoryResponse`、`HealthResponse`、`InfoResponse`）in `ibios-web/src/main/java/com/ibios/web/model/`
- [x] T007 新建 `WebConfig`（SPA 回落 `/admin/**`→`admin/index.html`，不影响 `/api/**`） in `ibios-web/src/main/java/com/ibios/web/config/WebConfig.java`
- [x] T008 更新 `application.yaml`：在 `spring.autoconfigure.exclude` 追加 `org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration` in `ibios-boot/src/main/resources/application.yaml`

---

## Phase 2: US1 — Session 管理 + Agent 调用

> 目标：业务系统能通过 REST API 创建会话、发消息、查历史、归档，以及无状态 invoke。
> 独立可测：POST /sessions 返回 200 + sessionId；POST /sessions/{id}/messages 调 AgentService.process 返回结果。

- [x] T009 [US1] 新建 `SessionApiController`（4 个端点：POST /sessions、POST /sessions/{id}/messages、GET /sessions/{id}、DELETE /sessions/{id}） in `ibios-web/src/main/java/com/ibios/web/controller/SessionApiController.java`
- [x] T010 [US1] 新建 `AgentApiController`（1 个端点：POST /agents/{name}/invoke） in `ibios-web/src/main/java/com/ibios/web/controller/AgentApiController.java`

### US1 测试

- [x] T011 [US1] 新建 `SessionApiControllerTest`（@WebMvcTest 切片）：超 32KB→400、Session 不存在→404、正常请求 agentService.process 恰被调一次 in `ibios-web/src/test/java/com/ibios/web/controller/SessionApiControllerTest.java`

---

## Phase 3: US2 — 信息查询端点

> 目标：运营方能查询 Profile 列表、Tool 列表、MEMORY.md 全文。
> 独立可测：GET /profiles 返回 ProfileRegistry 内容；GET /tools 返回 ToolRegistry 内容；GET /memory 返回 MEMORY.md 全文。
> 依赖：US1 的异常处理和模型类已就位。

- [x] T012 [P] [US2] 新建 `ProfileQueryController`（GET /api/v1/profiles） in `ibios-web/src/main/java/com/ibios/web/controller/ProfileQueryController.java`
- [x] T013 [P] [US2] 新建 `ToolQueryController`（GET /api/v1/tools） in `ibios-web/src/main/java/com/ibios/web/controller/ToolQueryController.java`
- [x] T014 [US2] 新建 `MemoryQueryController`（GET /api/v1/memory，调 `LongTermMemoryStore.load()` 返回全文） in `ibios-web/src/main/java/com/ibios/web/controller/MemoryQueryController.java`

---

## Phase 4: US3 — 系统状态端点

> 目标：health 和 info 端点供外部监控系统探测。
> 独立可测：GET /health 返回 ok；GET /info 返回系统信息含 Provider 连通状态。
> 依赖：US1-US2 的异常处理和模型类已就位。

- [x] T015 [US3] 新建 `SystemController`（GET /api/v1/health、GET /api/v1/info） in `ibios-web/src/main/java/com/ibios/web/controller/SystemController.java`

---

## Phase 5: 跨切面测试

> 目标：GlobalExceptionHandler 映射正确性 + 真实上下文冒烟。

- [x] T016 新建 `GlobalExceptionHandlerTest`（@WebMvcTest 切片）：每类异常→约定状态码；响应体都是 ApiResponse 信封；500 时响应不含内部异常 message in `ibios-web/src/test/java/com/ibios/web/exception/GlobalExceptionHandlerTest.java`
- [x] T017 新建 `WebSmokeIT`（@SpringBootTest + @Tag("integration")）：/health、/info、/profiles、/tools 真实链路可达 in `ibios-web/src/test/java/com/ibios/web/WebSmokeIT.java`

---

## Phase 6: US4 — 管理平台前端

> 目标：Vue 3 + Vite 只读管理台，五页调 /api/v1 端点，视觉风格与官网首页一致。
> 独立可测：npm run build 产出到 src/main/resources/static/admin/；/admin 路径可访问。

- [x] T018 [US4] 创建 Vue 3 + Vite 前端工程骨架（package.json、vite.config.js、index.html、main.js、App.vue、router） in `ibios-web/src/main/frontend/`
- [x] T019 [US4] 实现 Sidebar 组件 + 五个页面（SessionsView、ProfilesView、ToolsView、MemoryView、InfoView），深色主题 + 橙色主色，只读无写操作 in `ibios-web/src/main/frontend/src/`
- [x] T020 [US4] 创建风格 skill `.claude/skills/ibios-admin-ui/SKILL.md`（固化设计 token + 工程约定 + 验收清单） in `.claude/skills/ibios-admin-ui/SKILL.md`
- [x] T021 执行 `npm run build`，产出静态资源到 `ibios-web/src/main/resources/static/admin/` in `ibios-web/src/main/frontend/`

---

## Phase 7: 验证

- [x] T022 跑 `mvn test -pl ibios-web` 确认所有单测绿
- [x] T023 跑 `mvn clean verify` 全绿（含 P3C/SpotBugs/PMD 门禁）

---

## 依赖关系

```
Phase 1 (T001-T008)
  ├── Phase 2 (T009-T011) — US1 Session + Agent
  ├── Phase 3 (T012-T014) — US2 信息查询
  ├── Phase 4 (T015)      — US3 系统状态
  └── Phase 5 (T016-T017) — 跨切面测试
Phase 6 (T018-T021)       — US4 前端（可与 Phase 2-5 并行）
Phase 7 (T022-T023)       — 验证（全部完成后）
```

**并行机会**：
- T001-T004 四个异常类可并行（不同文件）
- T012-T013 两个查询 Controller 可并行（不同文件）
- Phase 6 前端工程可与 Phase 2-5 后端并行

---

## 交付物核对（↔ 课件"本节交付物"）

### 代码

| 课件要求 | 任务 | 状态 |
|----------|------|------|
| SessionApiController | T009 | ☐ |
| AgentApiController | T010 | ☐ |
| ProfileQueryController | T012 | ☐ |
| ToolQueryController | T013 | ☐ |
| MemoryQueryController | T014 | ☐ |
| SystemController | T015 | ☐ |
| GlobalExceptionHandler（扩展） | T005 | ☐ |
| springdoc-openapi 集成 | 已有依赖，T008 配 autoconfigure.exclude | ☐ |

### 测试

| 课件要求 | 任务 | 状态 |
|----------|------|------|
| SessionApiControllerTest | T011 | ☐ |
| GlobalExceptionHandlerTest | T016 | ☐ |
| WebSmokeIT | T017 | ☐ |

### 配置

| 课件要求 | 任务 | 状态 |
|----------|------|------|
| spring.threads.virtual.enabled=true | 已有（application.yaml） | ✓ |
| 端口 8080 | 已有（application.yaml） | ✓ |
| 消息 32KB 限制 | T009 Controller 内校验 | ☐ |
| 历史 100 条限制 | T009 Controller 内截断 | ☐ |
| 排除 OpenAiAutoConfiguration | T008 | ☐ |

### 前端

| 课件要求 | 任务 | 状态 |
|----------|------|------|
| Vue 3 + Vite 工程 | T018 | ☐ |
| 五个页面（只读） | T019 | ☐ |
| SPA 回落 WebConfig | T007 | ☐ |
| npm run build 产出 | T021 | ☐ |
| 风格 skill ibios-admin-ui | T020 | ☐ |

---

## 实现策略

**MVP 范围**：Phase 1-5 + Phase 7（后端 10 个端点 + 测试全绿）。前端 Phase 6 可后续补齐。

**增量交付**：
1. Phase 1 建完基础设施后，Phase 2-4 的 Controller 可并行开发
2. 每个 Controller 写完即可手动 curl 验证
3. 测试 Phase 5 的 GlobalExceptionHandlerTest 是"门面分寸"回归的关键——500 不泄漏内部细节
4. WebSmokeIT 打 @Tag("integration")，CI 跳过，本地手动跑

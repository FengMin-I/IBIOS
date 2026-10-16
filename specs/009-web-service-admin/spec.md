# Feature Specification: Web Service 与第一版管理平台

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: Web Service 与第一版管理平台——IBIOS 的对外 REST API 门面
- **Lesson**: 26
- **Author**: IBIOS Dev
- **Date**: 2026-10-15
- **Status**: draft

---

## 1. Background & Value

到第 25 节为止，IBIOS 底座能力全部就位（Provider、ReAct、CLI、Notify、Tool、Memory、Sandbox、Scheduler），但只有 CLI 一个对外出口——业务系统没法用。Web Service 把内部能力包装成 REST API，让任何能发 HTTP 请求的业务系统都能把 Agent 接进自己的流程。同时交付第一版只读管理平台（Vue 3 + Vite），调 API 渲染页面，验证"API 是完备的对外通道"这件事本身。

---

## 2. User Scenarios

### Scenario 1: 业务系统连续对话

业务系统要跟 Agent 多轮对话：先 POST /sessions 创建会话，拿到 session ID，再多次 POST /sessions/{id}/messages 发消息。每次发消息都走 AgentService.process 同一个引擎，会话历史在 Session 里自然累积。业务系统不需要关心 ReAct 循环、Tool 执行这些内部细节，只管发 HTTP 请求。

### Scenario 2: 业务系统一次性调用

业务系统只想调一次 Agent 拿个结果，不想管会话状态：直接 POST /agents/{name}/invoke，带上消息内容，系统临时建一个 Session、跑完 ReAct 循环、返回结果、不留状态。适合无状态的短任务，比如"帮我翻译这句话""总结一下这段文本"。

### Scenario 3: 运营方查看运行状态

运营方要看看系统跑得怎么样：访问 http://localhost:8080/admin，管理平台展示五个页面——会话列表、Profile 列表、Tool 列表、长期记忆（MEMORY.md 全文）、系统状态（含各 Provider 连通性）。只读，不能创建或修改 Agent（那是 29/30 节的事）。所有数据都来自 /api/v1 的 GET 端点，顺带验证了"API 是完备的对外通道"。

---

## 3. Functional Requirements

Each requirement MUST be testable.

- **FR1**: 系统暴露 10 个 REST 端点，统一前缀 /api/v1，按资源分成六个 Controller（Session/Agent/Profile/Memory/Tool/System）。
- **FR2**: Session 管理 4 个端点：POST /sessions（创建会话）、POST /sessions/{id}/messages（发消息，调 AgentService.process）、GET /sessions/{id}（查历史，最多返回最近 100 条）、DELETE /sessions/{id}（归档会话）。
- **FR3**: Agent 调用 1 个端点：POST /agents/{name}/invoke（无状态调用，临时建 Session、跑完返回、不留状态）。
- **FR4**: 信息查询 3 个端点：GET /profiles（列 ProfileRegistry 所有 Agent 配置）、GET /tools（列 ToolRegistry 所有可用工具）、GET /memory（读 MEMORY.md 全文）。
- **FR5**: 系统状态 2 个端点：GET /health（返回 ok 表示系统存活）、GET /info（返回系统信息，含各 Provider 连通状态）。
- **FR6**: 统一异常出口 GlobalExceptionHandler：400 参数错误、404 资源不存在、500 内部错误（不泄漏内部异常细节，对外只说"内部错误"）、503 Provider 故障、504 Agent 调用超时（60 秒上限）。所有错误响应复用既有 ApiResponse 信封（code/message/data/timestamp）。
- **FR7**: 请求大小限制：单条消息最大 32KB，超限返回 400。
- **FR8**: springdoc-openapi 集成，/swagger-ui 自动生成接口文档，不用手写接口文档。
- **FR9**: 管理平台前端：Vue 3 + Vite 单页应用，源码放 ibios-web/src/main/frontend/，npm run build 产出到 src/main/resources/static/admin/，Spring 托在 /admin 子路径。五个页面（会话/Profile/Tool/记忆/状态），只读，调 /api/v1 端点。视觉风格与官网首页一致（深色主题 + 橙色主色 #f97316，Inter + JetBrains Mono 字体）。
- **FR10**: SPA 路径回落：/admin/** 未命中路径回落 admin/index.html（前端路由需要），GET /api/v1/** 不受影响（API 端点正常返回 JSON）。
- **FR11**: spring.threads.virtual.enabled=true，每个 HTTP 请求跑在独立虚拟线程上，LLM 调用这种 IO 等待时自动让出，不引入 WebFlux、不写响应式代码。
- **FR12**: 排除 Spring AI eager 自动装配：application.yaml 的 spring.autoconfigure.exclude 加 OpenAiAutoConfiguration（deepseek 走 spring-ai-openai starter，它的自动配置会急切实例化 ChatModel 并索要 spring.ai.openai.api-key；IBIOS 不用这个自动 Bean，Provider 由 ProviderChatModelFactory 显式构造），serve 只需 DEEPSEEK_API_KEY 一个环境变量就能起得来。

---

## 4. Out of Scope (Boundary)

Explicitly list what this feature does NOT do.

- 认证机制（假设内网部署，扩展阶段补 API Key + JWT）
- SSE 流式响应、WebSocket
- 限流、RBAC 权限
- Profile/Memory 的增删改端点（29/30 节补齐，本节只做只读查询）
- Webhook 触发、Prometheus metrics（扩展阶段）
- 通过 API 定义一个 Agent（29/30 节的正题，本节刻意留白）

---

## 5. Acceptance Criteria

### Automated (Harness)

Tests that `mvn test` / `mvn clean verify` will run. List test classes and what they verify.

- **SessionApiControllerTest**（@WebMvcTest 切片，只起 MVC 层、mock AgentService）：
  - 消息超 32KB → 400
  - Session 不存在 → 404
  - 正常请求 agentService.process 恰被调一次（Controller 没夹带私货，薄）
- **GlobalExceptionHandlerTest**（@WebMvcTest 切片）：
  - 每类异常映射到约定状态码（400/404/500/503/504）
  - 响应体都是统一 ApiResponse 信封
  - 500 时响应里不含内部异常的 message（不泄漏，对外只说"内部错误"）
- **WebSmokeIT**（@SpringBootTest 起真实上下文，打 @Tag("integration") CI 跳过，不依赖模型）：
  - /health、/info、/profiles、/tools 真实链路可达——验证 Bean 装配和扫描范围没炸
  - 会真实触发 JPA repository 扫描，18 节那个 "Found 0 repositories" 的坑如果在 web 模块复发，这里第一时间红

关键回归点：Controller 薄（不夹带私货）、异常单出口、500 不泄漏内部细节。

### Manual

Items requiring human verification — real model calls, real webhooks, etc.

- 10 个端点 curl 真链路全走通（含真模型的 POST /sessions/{id}/messages），审计有账（llm_calls/tool_invocations 表有记录）。
- CLI 聊过的 Session 从 GET /sessions/{id} 能查到——两个人推入口（CLI + Web）共享存储。
- 断掉 Provider 拿 503、构造超 60 秒调用拿 504（这两个依赖真实故障注入，切片测试模拟不了完整链路）。
- 并发压一把（200 并发 invoke），虚拟线程扛得住。
- 管理平台五个页面渲染真实数据、无写操作入口；/swagger-ui 文档齐全。
- 400/404 映射、错误格式统一、500 不泄漏——已由 harness 覆盖，mvn test 绿即打勾。

---

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- **Lesson 16**: ProfileRegistry（Agent 配置注册表，GET /profiles 列出来）
- **Lesson 17**: AgentService（统一处理入口，POST /sessions/{id}/messages 和 POST /agents/{name}/invoke 都调它）
- **Lesson 18**: SessionManager（会话管理，POST /sessions 创建、GET /sessions/{id} 查询、DELETE /sessions/{id} 归档）
- **Lesson 20**: ToolRegistry（工具注册表，GET /tools 列出来）
- **Lesson 22**: MemoryService / MEMORY.md 读取（GET /memory 返回全文）
- **Lesson 24**: ApiResponse 信封、GlobalExceptionHandler 骨架（本节复用并扩展）

### External Dependencies

- springdoc-openapi-starter-webmvc-ui（已在 ibios-web pom.xml，自动生成 /swagger-ui）
- Vue 3 + Vite（前端工程，放 ibios-web/src/main/frontend/，npm run build 产出静态资源）
- DEEPSEEK_API_KEY 环境变量（serve 启动必需，排掉 OpenAiAutoConfiguration 后不需要 spring.ai.openai.api-key）

### Assumptions

- 核心阶段假设内网部署，不做认证（扩展阶段补 API Key + JWT）
- 单条消息最大 32KB 是防呆不是治理，Session 历史返回最多 100 条同理
- Agent 调用最长 60 秒超时，超时返回 504
- 管理平台这版只读，不能创建或修改 Agent（29/30 节补齐）
- 前端视觉风格与官网首页完全一致（深色主题 + 橙色主色，Inter + JetBrains Mono 字体），不引外部 UI 库，用项目内 skill `ibios-admin-ui` 固化设计 token

---

## Constitution Check

Verify this spec complies with `.specify/memory/constitution.md`:

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith（Web Service 是 Spring Boot 标准用法）
- [x] Principle 2: Five core capabilities priority（Web Service 是能力五，核心阶段必须交付）
- [x] Principle 3: Self-implemented ReAct loop（Controller 只调 AgentService，不碰 ReActLoop 内部）
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED（排除 OpenAiAutoConfiguration，不用 Spring AI 的自动 Bean）
- [x] Principle 5: Plugin tool three-tier access（GET /tools 列 ToolRegistry，不区分工具来源）
- [x] Principle 6: Synchronous + virtual threads（spring.threads.virtual.enabled=true，不用 WebFlux）
- [x] Principle 7: Sandbox interface-first（Controller 不直接调 Tool，走 AgentService → ToolExecutor → Sandbox 路径）
- [x] Principle 8: SQLite + audit day-one（Web 触发的调用同样落 llm_calls/tool_invocations，审计路径与人推一致）
- [x] Principle 9: Explicit provider mapping（不影响，GET /info 查 Provider 状态走既有 ProviderService）
- [x] Principle 10: Global invariants（①涉外 IO 过 Sandbox ②审计完整 ③无明文 key ④session_id 在 SessionManager 内拼接 ⑤无 Reactor ⑥无 Spring AI auto-exec）

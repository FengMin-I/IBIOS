# Implementation Plan: Web Service 与第一版管理平台

## 1. Overview

**Feature**: Web Service 与第一版管理平台——IBIOS 的对外 REST API 门面
**Spec**: specs/009-web-service-admin/spec.md
**Lesson**: 26
**Date**: 2026-10-15

IBIOS 底座能力全部就位，但只有 CLI 一个对外出口。本节交付 Web Service（10 个 REST 端点）和第一版只读管理平台（Vue 3 + Vite），让业务系统能通过 HTTP 接入 Agent，运营方能通过浏览器查看运行状态。

---

## 2. Technical Stack

- **JDK**: 21
- **Framework**: Spring Boot 3.x (Spring MVC)
- **Virtual Threads**: spring.threads.virtual.enabled=true
- **API Docs**: springdoc-openapi-starter-webmvc-ui (/swagger-ui)
- **Frontend**: Vue 3 + Vite (深色主题 + 橙色主色 #f97316)
- **Build**: Maven (后端) + npm (前端)
- **Database**: SQLite + Spring Data JPA (既有)

---

## 3. Module Placement

| Component | Module | Package |
|-----------|--------|---------|
| SessionApiController | ibios-web | com.ibios.web.controller |
| AgentApiController | ibios-web | com.ibios.web.controller |
| ProfileQueryController | ibios-web | com.ibios.web.controller |
| MemoryQueryController | ibios-web | com.ibios.web.controller |
| ToolQueryController | ibios-web | com.ibios.web.controller |
| SystemController | ibios-web | com.ibios.web.controller |
| GlobalExceptionHandler (扩展) | ibios-web | com.ibios.web.exception |
| SessionNotFoundException | ibios-web | com.ibios.web.exception |
| AgentNotFoundException | ibios-web | com.ibios.web.exception |
| AgentTimeoutException | ibios-web | com.ibios.web.exception |
| WebConfig (SPA 回落) | ibios-web | com.ibios.web.config |
| 请求/响应模型类 | ibios-web | com.ibios.web.model |
| Vue 3 前端工程 | ibios-web | src/main/frontend/ |
| 静态资源产出 | ibios-web | src/main/resources/static/admin/ |

---

## 4. Configuration

### application.yaml 新增/修改

```yaml
server:
  port: 8080

spring:
  threads:
    virtual:
      enabled: true
  autoconfigure:
    exclude:
      - com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAutoConfiguration
      - org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration

# 请求大小限制（Spring Boot 内置）
spring.servlet.multipart.max-file-size: 32KB
spring.servlet.multipart.max-request-size: 32KB
```

### 常量定义

| 常量 | 值 | 说明 |
|------|-----|------|
| MAX_MESSAGE_SIZE | 32 * 1024 (32KB) | 单条消息最大大小 |
| MAX_HISTORY_SIZE | 100 | Session 历史返回最多条数 |
| AGENT_TIMEOUT_SECONDS | 60 | Agent 调用超时时间 |
| ADMIN_BASE_PATH | /admin | 管理平台基础路径 |

---

## 5. API Endpoints

### 5.1 Session 管理 (SessionApiController)

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | /api/v1/sessions | 创建会话 | {} | SessionResponse |
| POST | /api/v1/sessions/{id}/messages | 发消息 | MessageRequest | MessageResponse |
| GET | /api/v1/sessions/{id} | 查历史 | - | SessionResponse |
| DELETE | /api/v1/sessions/{id} | 归档会话 | - | Void |

### 5.2 Agent 调用 (AgentApiController)

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | /api/v1/agents/{name}/invoke | 无状态调用 | InvokeRequest | InvokeResponse |

### 5.3 信息查询

| Method | Path | Controller | Response |
|--------|------|------------|----------|
| GET | /api/v1/profiles | ProfileQueryController | List<ProfileSummary> |
| GET | /api/v1/tools | ToolQueryController | List<ToolSummary> |
| GET | /api/v1/memory | MemoryQueryController | MemoryResponse |

### 5.4 系统状态 (SystemController)

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | /api/v1/health | 健康检查 | HealthResponse |
| GET | /api/v1/info | 系统信息 | InfoResponse |

---

## 6. Error Handling

### 6.1 异常映射

| Exception | HTTP Status | Error Code | Message |
|-----------|-------------|------------|---------|
| IllegalArgumentException | 400 | BAD_REQUEST | 参数错误 |
| SessionNotFoundException | 404 | NOT_FOUND | Session 不存在 |
| AgentNotFoundException | 404 | NOT_FOUND | Agent 不存在 |
| ResourceNotFoundException | 404 | NOT_FOUND | 资源不存在 |
| AgentTimeoutException | 504 | GATEWAY_TIMEOUT | Agent 调用超时 |
| IllegalStateException | 503 | SERVICE_UNAVAILABLE | Provider 故障 |
| ServiceUnavailableException | 503 | SERVICE_UNAVAILABLE | 服务不可用 |
| Exception (兜底) | 500 | INTERNAL_ERROR | Internal server error |

### 6.2 统一响应信封

```java
public class ApiResponse<T> {
  private int code;        // 错误码（0=成功）
  private String message;  // 消息
  private T data;          // 数据
  private long timestamp;  // 时间戳
}
```

### 6.3 安全分寸

- 500 响应对外只说"Internal server error"，不泄漏 e.getMessage()
- 内部异常细节进日志（log.error），不进响应体
- 连接串、堆栈信息这类内幕不能漏给外部

---

## 7. Frontend Architecture

### 7.1 工程结构

```
ibios-web/src/main/frontend/
├── src/
│   ├── views/
│   │   ├── SessionsView.vue    # 会话列表
│   │   ├── ProfilesView.vue    # Profile 列表
│   │   ├── ToolsView.vue       # Tool 列表
│   │   ├── MemoryView.vue      # 长期记忆
│   │   └── InfoView.vue        # 系统状态
│   ├── components/
│   │   └── Sidebar.vue         # 左侧导航
│   ├── router/
│   │   └── index.js            # Vue Router
│   ├── App.vue
│   └── main.js
├── package.json
├── vite.config.js
└── index.html
```

### 7.2 Vite 配置

```javascript
// vite.config.js
export default {
  base: '/admin/',
  build: {
    outDir: '../resources/static/admin',
    emptyOutDir: true
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
}
```

### 7.3 SPA 回落

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/admin/**")
      .addResourceLocations("classpath:/static/admin/")
      .resourceChain(true)
      .addResolver(new PathResourceResolver() {
        @Override
        protected Resource resolveResourceInternal(...) {
          Resource requested = super.resolveResourceInternal(...);
          return (requested == null) ? new ClassPathResource("/static/admin/index.html") : requested;
        }
      });
  }
}
```

### 7.4 视觉规范

- **深色主题**: 背景 #000000 / 卡片 #111111 / 悬浮块 #1a1a1a
- **主色（橙）**: #f97316 为主、hover/强调用 #ea6a00 / #c2550a
- **文字**: 主 #f5f5f5 / 次 #a3a3a3 / 弱 #666666
- **字体**: 正文 Inter，代码/ID/JSON 用 JetBrains Mono（等宽）
- **布局**: 左侧竖直导航（深色）+ 右侧内容区
- **圆角**: 4-6px，整体克制、留白足

---

## 8. Test Strategy

### 8.1 Unit Tests (默认跑)

**SessionApiControllerTest** (@WebMvcTest)
- 超 32KB → 400
- Session 不存在 → 404
- 正常请求 agentService.process 恰被调一次

**GlobalExceptionHandlerTest** (@WebMvcTest)
- 每类异常映射到约定状态码
- 响应体都是统一 ApiResponse 信封
- 500 时响应里不含内部异常的 message

### 8.2 Integration Tests (@Tag("integration") CI 跳过)

**WebSmokeIT** (@SpringBootTest)
- /health、/info、/profiles、/tools 真实链路可达
- 验证 Bean 装配和扫描范围没炸

---

## 9. Implementation Phases

### Phase 1: 异常与模型 (T001-T005)

- T001: 新增 SessionNotFoundException, AgentNotFoundException, AgentTimeoutException
- T002: 扩展 GlobalExceptionHandler 新增异常映射
- T003: 新增请求/响应模型类
- T004: 新增 WebConfig (SPA 回落)
- T005: 更新 application.yaml (排除 OpenAiAutoConfiguration)

### Phase 2: Controller (T006-T011)

- T006: SessionApiController (4 个端点)
- T007: AgentApiController (1 个端点)
- T008: ProfileQueryController (GET /profiles)
- T009: MemoryQueryController (GET /memory)
- T010: ToolQueryController (GET /tools)
- T011: SystemController (GET /health, GET /info)

### Phase 3: 测试 (T012-T014)

- T012: SessionApiControllerTest
- T013: GlobalExceptionHandlerTest
- T014: WebSmokeIT (@Tag("integration"))

### Phase 4: 前端 (T015-T018)

- T015: 创建 Vue 3 + Vite 工程 (ibios-web/src/main/frontend/)
- T016: 实现五个页面 (Sessions/Profiles/Tools/Memory/Info)
- T017: 配置 vite.config.js (base: '/admin/', outDir: static/admin/)
- T018: npm run build 产出静态资源

### Phase 5: 验证 (T019-T020)

- T019: mvn test 全绿
- T020: mvn clean verify 全绿

---

## 10. Dependencies

### 既有依赖

- Lesson 16: ProfileRegistry
- Lesson 17: AgentService
- Lesson 18: SessionManager
- Lesson 20: ToolRegistry
- Lesson 22: MemoryService / MEMORY.md
- Lesson 24: ApiResponse, GlobalExceptionHandler 骨架

### 外部依赖

- springdoc-openapi-starter-webmvc-ui (已在 ibios-web pom.xml)
- Vue 3 + Vite (前端工程)
- DEEPSEEK_API_KEY 环境变量

---

## 11. Constitution Compliance

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith
- [x] Principle 2: Five core capabilities priority (Web Service 是能力五)
- [x] Principle 3: Self-implemented ReAct loop (Controller 只调 AgentService)
- [x] Principle 4: Spring AI protocol only, auto-exec DISABLED (排除 OpenAiAutoConfiguration)
- [x] Principle 5: Plugin tool three-tier access (GET /tools 列 ToolRegistry)
- [x] Principle 6: Synchronous + virtual threads (spring.threads.virtual.enabled=true)
- [x] Principle 7: Sandbox interface-first (Controller 不直接调 Tool)
- [x] Principle 8: SQLite + audit day-one (Web 触发同样落审计)
- [x] Principle 9: Explicit provider mapping (不影响)
- [x] Principle 10: Global invariants (六条全遵守)

---

## 12. Risk Mitigation

### Risk 1: SPA 回落拦截 API 端点

**缓解**: WebConfig 精确匹配 /admin/**，不影响 /api/**。测试覆盖。

### Risk 2: 前端构建产物未提交

**缓解**: .gitignore 排除 node_modules/，但包含 static/admin/。CI 脚本先 npm run build 再 mvn package。

### Risk 3: Agent 调用超时未处理

**缓解**: AgentTimeoutException 60 秒超时，返回 504。虚拟线程自动让出，不会耗尽线程池。

---

## 13. Success Criteria

### Functional

- 10 个 REST 端点全部可用
- 管理平台五个页面渲染真实数据
- /swagger-ui 文档齐全

### Non-Functional

- 单条消息最大 32KB，超限返回 400
- Session 历史返回最多 100 条
- Agent 调用最长 60 秒超时
- 500 响应不泄漏内部异常细节

### Quality

- mvn clean verify 全绿
- 三个测试类全部通过
- 无 P3C/SpotBugs/PMD 告警

---

## 14. Next Steps

1. 执行 `/speckit-tasks` 生成任务清单
2. 执行 `/speckit-implement` 实现
3. 执行 `/speckit-verify` 验收

# 任务清单：Agent Provider

> 由 `/speckit-tasks` 生成。按依赖排序，harness 优先。

## 元信息

- **功能**: Agent Provider — LLM 多模型路由与调用审计
- **计划**: `specs/001-agent-provider/plan.md`
- **日期**: 2026-10-13
- **状态**: pending

---

## 任务分类

- **[C]** — 配置 / Schema
- **[T]** — 测试（harness）
- **[I]** — 实现
- **[V]** — 验证

---

## Phase 1：Setup 与 Schema

- [ ] T001 [C] 更新 `schema.sql` — 重写 `llm_calls` 的 CREATE TABLE，新增 `session_id TEXT`、`success INTEGER NOT NULL DEFAULT 1`、`error_message TEXT` 列。文件：`ibios-boot/src/main/resources/schema.sql`
- [ ] T002 [C] 在 `application.yaml` 中新增 `ibios.providers` 配置段，含一个示例 provider 条目（name、api-key 环境变量占位、model）。移除无用的 `spring.ai.dashscope.api-key` 行。文件：`ibios-boot/src/main/resources/application.yaml`
- [ ] T003 [C] 新增 `ProvidersProperties` `@ConfigurationProperties(prefix = "ibios")` 类，含 `List<ProviderEntry> providers`。文件：`ibios-provider/src/main/java/com/ibios/provider/ProvidersProperties.java`

---

## Phase 2：基础 — Profile（ibios-core）

- [ ] T004 [P] [I] 创建 `Profile` record，含全部字段：name、description、identity（Identity record）、provider（ProviderConfig record）、tools、skills、mcpServers、channels、notifyChannels、schedules、bootstrap、settings。文件：`ibios-core/src/main/java/com/ibios/core/profile/Profile.java`
- [ ] T005 [P] [I] 创建嵌套 record：`Identity`、`ProviderConfig`、`ScheduleConfig`。文件：同 T004
- [ ] T006 [P] [I] 创建 `ProfileRegistry` — `Map<String, Profile>`，提供 `get(name)`、`getAll()`、`register()`。文件：`ibios-core/src/main/java/com/ibios/core/profile/ProfileRegistry.java`
- [ ] T007 [I] 创建 `ProfileLoader` — 扫描 `.ibios/profiles/*.yaml`，用 SnakeYAML 解析为 Profile，校验 provider.name 是否存在于全局 providers 列表（`ibios.providers`），坏文件记错误日志不阻断启动。文件：`ibios-core/src/main/java/com/ibios/core/profile/ProfileLoader.java`
- [ ] T008 [I] 创建 `ProviderNotFoundException` 运行时异常。文件：`ibios-core/src/main/java/com/ibios/core/profile/ProviderNotFoundException.java`
- [ ] T008a [P] [I] 创建 `IbiosTool` 接口——工具统一抽象（`name()`、`description()`、`inputSchema()` 返回 JSON Schema 字符串），内置工具和 MCP 工具均实现此接口。文件：`ibios-core/src/main/java/com/ibios/core/tool/IbiosTool.java`
- [ ] T009 [T] 创建 `ProfileLoaderTest` — 合法 YAML 全字段解析；未知 provider 清晰报错；坏文件不阻断其他；${ENV} 从环境变量解析。文件：`ibios-core/src/test/java/com/ibios/core/profile/ProfileLoaderTest.java`

---

## Phase 3：基础 — LlmCall 审计（ibios-storage）

- [ ] T010 [P] [I] 创建 `LlmCall` JPA 实体，匹配 schema.sql 列。文件：`ibios-storage/src/main/java/com/ibios/storage/audit/LlmCall.java`
- [ ] T011 [P] [I] 创建 `LlmCallRepository` 继承 `JpaRepository<LlmCall, Long>`。文件：`ibios-storage/src/main/java/com/ibios/storage/audit/LlmCallRepository.java`
- [ ] T012 [P] [I] 创建 `LlmCallAudit` 接口，含 `record(sessionId, provider, model, usage, success, errorMessage, latencyMs)` 方法。文件：`ibios-storage/src/main/java/com/ibios/storage/audit/LlmCallAudit.java`
- [ ] T013 [I] 创建 `JpaLlmCallAudit` 实现 `LlmCallAudit` — 写入 LlmCallRepository。文件：`ibios-storage/src/main/java/com/ibios/storage/audit/JpaLlmCallAudit.java`
- [ ] T014 [T] 创建 `LlmCallRepositoryTest` — 使用 schema.sql 建表，验证 CRUD，断言 success/error_message 列存在。文件：`ibios-storage/src/test/java/com/ibios/storage/audit/LlmCallRepositoryTest.java`

---

## Phase 4：Provider Service（ibios-provider）

- [ ] T015 [I] 创建 `ProviderChatModelFactory` — `@Component`，读取 `ProvidersProperties`，为每个 provider 条目创建 `DashScopeChatModel`，暴露 `Map<String, ChatModel>`。文件：`ibios-provider/src/main/java/com/ibios/provider/ProviderChatModelFactory.java`
- [ ] T016 [I] 创建 `ToolSchemaAdapter` — 将 `IbiosTool` 列表转换为 Spring AI `ToolCallback` 列表（name、description、input schema）。只翻译，不含执行逻辑。文件：`ibios-provider/src/main/java/com/ibios/provider/ToolSchemaAdapter.java`
- [ ] T016a [P] [I] 创建 `ProviderRequest` record——`chat()` 入参，含 `content`（String，发给模型的文本）+ `availableTools`（`List<IbiosTool>`，可空 = 不带工具）。文件：`ibios-provider/src/main/java/com/ibios/provider/ProviderRequest.java`
- [ ] T016b [P] [I] 创建 `ProviderResponse` record——`chat()` 出参，含 `text`（String，模型文本回复）+ `toolCalls`（`List<ToolCall>`，工具调用请求）+ `usage`（token 用量）。文件：`ibios-provider/src/main/java/com/ibios/provider/ProviderResponse.java`
- [ ] T017 [I] 创建 `ProviderService` — 构造函数接受 `Map<String, ChatModel>`、`ToolSchemaAdapter`、`LlmCallAudit`。单一方法 `ProviderResponse chat(String sessionId, Profile profile, ProviderRequest request)`：按 provider name 查找模型、翻译工具、调用模型、记录审计（成功 + 失败路径）、返回 `ProviderResponse`。文件：`ibios-provider/src/main/java/com/ibios/provider/ProviderService.java`
- [ ] T018 [T] 创建 `ProviderServiceTest` — 3 个关键回归测试：(1) 双 provider 路由不串台，(2) 调用失败记录 success=false 审计后重新抛出，(3) 带工具调用验证直接使用 ChatModel（非 ChatClient）。文件：`ibios-provider/src/test/java/com/ibios/provider/ProviderServiceTest.java`
- [ ] T019 [T] 创建 `ToolSchemaAdapterTest` — IbiosTool schema 翻译为 Spring AI 格式，字段逐一对齐；输出不含执行逻辑。文件：`ibios-provider/src/test/java/com/ibios/provider/ToolSchemaAdapterTest.java`

---

## Phase 5：集成冒烟测试

- [ ] T020 [T] 创建 `ProviderSmokeIT` — `@Tag("integration")`，从环境变量读 DASHSCOPE_API_KEY，通过 ProviderService 发起真实调用，断言响应非空且 llm_calls 新增 success=true 记录。文件：`ibios-provider/src/test/java/com/ibios/provider/ProviderSmokeIT.java`

---

## Phase 6：验证

- [ ] T021 [V] 运行 `mvn clean verify` — 所有模块编译、所有测试通过、所有静态检查门禁通过
- [ ] T022 [V] Grep 审计 — `grep -rn "sk-\|dashscope-[a-zA-Z0-9]\{20,\}"` 找不到明文密钥
- [ ] T023 [V] 跨模块依赖检查 — ibios-core 无 ibios-provider/storage 导入；ibios-provider 依赖 ibios-core；ibios-storage 依赖 ibios-core

---

## 交付物清单

与课件第 16 节"本节交付物"交叉对照：

### 代码
- [ ] `Profile` record（T004-T005）
- [ ] `ProfileLoader`（T007）
- [ ] `ProfileRegistry`（T006）
- [ ] `ProviderService` 含 `chat(sessionId, Profile, ProviderRequest)`（T017）
- [ ] `ToolSchemaAdapter`（T016）
- [ ] `LlmCall` 实体 + `LlmCallRepository`（T010-T011）
- [ ] `LlmCallAudit` 接口 + `JpaLlmCallAudit`（T012-T013）
- [ ] `ProviderChatModelFactory`（T015）
- [ ] `ProvidersProperties`（T003）
- [ ] `ProviderNotFoundException`（T008）
- [ ] `IbiosTool` 接口（T008a）
- [ ] `ProviderRequest` + `ProviderResponse` record（T016a, T016b）

### 测试
- [ ] `ProfileLoaderTest`（T009）
- [ ] `ProviderServiceTest`（T018）
- [ ] `ToolSchemaAdapterTest`（T019）
- [ ] `LlmCallRepositoryTest`（T014）
- [ ] `ProviderSmokeIT`（T020）

### 配置
- [ ] `application.yaml` `ibios.providers` 配置段（T002）

### Schema
- [ ] `llm_calls` 表含 `success`/`error_message`/`session_id` 列（T001）

---

## 依赖图

```
Phase 1（Schema/Config）
  ↓
Phase 2（Profile + IbiosTool） ← T009（ProfileLoaderTest）
  ↓
Phase 3（LlmCall 审计） ← T014（LlmCallRepositoryTest）
  ↓
Phase 4（ProviderService） ← T018（ProviderServiceTest）、T019（ToolSchemaAdapterTest）
  ↓
Phase 5（冒烟测试） ← T020
  ↓
Phase 6（验证）
```

**可并行机会**：T004/T005/T006（Profile record + Registry）可并行编写。T010/T011/T012（LlmCall 实体 + repo + 审计接口）可并行编写。

## MVP 范围

最小可验证范围：T001 + T002 + T004-T008 + T008a + T010-T013 + T015-T017 + T016a-T016b + T009 + T014 + T018 + T021。这给出一个可工作的 Provider，含 Profile 加载、LLM 调用路由和审计——不含冒烟测试。

# 实施计划：Agent Provider

> 由 `/speckit-plan` 创建。

## 元信息

- **功能**: Agent Provider — LLM 多模型路由与调用审计
- **规格**: `specs/001-agent-provider/spec.md`
- **日期**: 2026-10-13
- **状态**: draft

---

## 1. 技术栈

JDK 21 + Spring Boot 3.x + Spring AI Alibaba（动手前先跑 `mvn dependency:tree` 确认锁定 BOM 里目标依赖存在）+ SQLite + Spring Data JPA。凭证走环境变量占位，不落明文。SQLite 用手工建表脚本，不依赖 `hibernate.ddl-auto=update`。

**已验证依赖**（来自 pom.xml）：
- `spring-ai-bom` 1.0.0
- `spring-ai-alibaba-bom` 1.0.0.2
- `spring-ai-alibaba-starter-dashscope`（在 ibios-provider/pom.xml 中）
- `sqlite-jdbc` 3.47.0.0
- `hibernate-community-dialects` 6.5.2.Final
- `snakeyaml`（Spring Boot 传递依赖）

## 2. 模块落位

| 类 / 包 | 目标模块 |
|---------|---------|
| `Profile`（record） | ibios-core（`com.ibios.core.profile`） |
| `ProfileLoader` | ibios-core（`com.ibios.core.profile`） |
| `ProfileRegistry` | ibios-core（`com.ibios.core.profile`） |
| `ProviderNotFoundException` | ibios-core（`com.ibios.core.profile`） |
| `ProviderService` | ibios-provider（`com.ibios.provider`） |
| `ToolSchemaAdapter` | ibios-provider（`com.ibios.provider`） |
| `ProviderChatModelFactory` | ibios-provider（`com.ibios.provider`） |
| `ProvidersProperties` | ibios-provider（`com.ibios.provider`） |
| `LlmCall`（JPA 实体） | ibios-storage（`com.ibios.storage.audit`） |
| `LlmCallRepository` | ibios-storage（`com.ibios.storage.audit`） |
| `LlmCallAudit`（审计写入接口 + 实现） | ibios-storage（`com.ibios.storage.audit`） |
| `schema.sql`（更新） | ibios-boot/src/main/resources |
| `application.yaml`（新增 `ibios.providers`） | ibios-boot/src/main/resources |

## 3. 设计决策

### 决策 1：ChatModel Bean 创建策略

**问题**：`application.yaml` 中已排除全部 7 个 DashScope 自动配置。Spring AI Alibaba 不会自动创建任何 `ChatModel` bean。需要手动创建。

**选择**：`ProviderChatModelFactory` — 一个 `@Component`，读取 `ibios.providers` 配置，为每个 provider 条目用 `DashScopeChatModel` 构造函数创建一个 `ChatModel`，暴露 `Map<String, ChatModel>`。

**理由**：显式工厂完全控制 bean 创建。配置中每个 provider 条目精确映射到一个 ChatModel。无魔法，无类型扫描。

**已考虑的替代方案**：
- 重新启用 `DashScopeChatAutoConfiguration` 并用 `@Qualifier` — 否决：失去控制，耦合 Spring AI 内部 bean 命名
- 每个 provider 一个 `@Bean` 方法 — 否决：不可扩展，配置驱动更好

### 决策 2：全局配置结构（`ibios.providers`）

**问题**：课件指定 `ibios.providers` 为全局 provider 配置层。当前 `application.yaml` 使用 `spring.ai.dashscope.api-key`。

**选择**：在 `application.yaml` 中新增 `ibios.providers` 列表。每个条目有 `name` 和 `api-key`（环境变量占位）。移除 `spring.ai.dashscope.api-key` 行（自动配置已排除，用不到）。

```yaml
ibios:
  providers:
    - name: dashscope
      api-key: ${DASHSCOPE_API_KEY:}
      model: qwen-plus
```

**理由**：匹配课件架构。配置驱动，支持多 provider。`spring.ai.dashscope` 命名空间因自动配置已排除而未使用。

### 决策 3：Profile 用 Java Record

**问题**：Profile 需要不可变、携带多个字段、兼容 SnakeYAML。

**选择**：`Profile` 用 Java `record`，包含所有字段。SnakeYAML 可以反序列化到 record（Java 16+）。

**理由**：天然不可变。语法紧凑。Record 在 SnakeYAML 2.0+（Spring Boot 3.x 自带 2.2+）中原生支持。

### 决策 4：LlmCall Schema 更新

**问题**：现有 `llm_calls` 表缺少 FR8/FR9 要求的 `success` 和 `error_message` 列。

**选择**：在 `schema.sql` 中新增 `success INTEGER NOT NULL DEFAULT 1` 和 `error_message TEXT` 列。同时新增 `session_id TEXT` 用于审计追溯（课件 chat 方法签名中提到 sessionId）。

**理由**：SQLite ALTER TABLE 受限但 ADD COLUMN 可用。手工建表脚本保证生产一致性。

### 决策 5：审计写入接口

**问题**：ProviderService 需要写审计记录。直接依赖 JPA Repository 吗？

**选择**：`LlmCallAudit` 接口放在 ibios-storage，由同模块的 `JpaLlmCallAudit` 实现。ProviderService 依赖接口。

**理由**：依赖方向干净。ProviderService 不知道 JPA。测试可以 mock 接口。

## 4. 测试策略

测试按课件"验收 harness"执行：`ProfileLoaderTest`、`ProviderServiceTest`、`ToolSchemaAdapterTest`、`LlmCallRepositoryTest`、`ProviderSmokeIT`，覆盖双 provider 路由不串台、失败审计 success=false、自动执行关闭、Profile 全字段解析、未知 provider 报错、坏文件不阻断、ENV 占位解析、schema 翻译对齐、llm_calls 表读写。单测默认跑；集成冒烟打 `@Tag("integration")` CI 跳过。实现完成的定义是 `mvn clean verify` 全绿。

## 5. 语法禁区

避开 P3C/ASM 解析不了的 Java 18+ 语法形态（如增强 switch 的 `default ->` 写法）。静态检查是构建门禁。

## 6. Constitution 合规检查

- [x] 原则 1：JDK 21 + Spring Boot 3.x 单体 — 所有模块遵循
- [x] 原则 2：五大核心能力优先 — Provider 是能力 #1
- [x] 原则 3：自研 ReAct 循环 — ProviderService.chat() 返回 Response，不含循环逻辑
- [x] 原则 4：Spring AI 仅做协议，自动执行禁用 — 决策 1 手动创建 ChatModel；FR6 由回归测试强制
- [x] 原则 5：插件工具三层接入 — ToolSchemaAdapter 处理翻译层
- [x] 原则 6：同步 + 虚拟线程 — 任何类中无 reactive 类型
- [x] 原则 7：Sandbox 接口先行 — 本节不适用（Sandbox 是第 24 节）
- [x] 原则 8：SQLite + 审计 Day-One — LlmCall 实体 + schema.sql 更新；成功和失败均记录
- [x] 原则 9：显式 Provider 映射 — ProviderChatModelFactory 显式构建 Map<String, ChatModel>
- [x] 原则 10：全局不变量 — FR9（审计完整性）、FR11（无明文密钥）、无 reactive 导入

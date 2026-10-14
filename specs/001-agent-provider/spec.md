# 功能规格：Agent Provider

> 由 `/speckit-specify` 创建。进入 clarify 前须填满所有章节。

## 元信息

- **功能**: Agent Provider — LLM 多模型路由与调用审计
- **课节**: 16
- **作者**: IBIOS 团队
- **日期**: 2026-10-13
- **状态**: draft

---

## 1. 背景与价值

Agent 需要与大语言模型对话，但不同厂商（DeepSeek、通义、Kimi 等）的 API 格式各不相同。Provider 就是 Agent 与 LLM 之间的前台：上层传入 Profile（用哪个 provider/模型/温度）和 Prompt，Provider 负责选模型、按厂商格式发送、返回结果。后续换模型只改配置，ReAct 循环不用动。Provider 只做协议翻译，**不执行工具**——工具执行归 ReActLoop + ToolExecutor 管。

## 2. 用户场景

### 场景 1：运行时模型切换

一个运维 Agent 日常对话配的是 DeepSeek。遇到复杂分析任务时，运行时切换到更强的模型（qwen-plus），无需改代码——只改 Profile 配置。

### 场景 2：工具 Schema 翻译但不执行

Agent 调 LLM 时，Provider 把 IBIOS 内部的工具格式翻译成 Spring AI 的工具 schema 发给模型。模型可能回复"我要调 http_get，参数是这些"。Provider 把这个请求原样返回给上层循环——**自己不执行工具**。

### 场景 3：失败审计留痕

LLM 调用失败时（超时、限流、模型报错），审计表 `llm_calls` 记录 `success=false` 及错误原因。即使异常向上抛出，事件也可追溯。

## 3. 功能需求

每条需求必须可测试。

- **FR1**: Profile 从 YAML 解析为 Java 对象，携带全部字段：`name`、`description`、`identity`、`provider`（含 `name`/`model`/`temperature`）、`tools`、`skills`、`mcp_servers`、`channels`、`notify_channels`、`schedules`、`bootstrap`、`settings`。后续课节按需消费各字段，但类在本节一次性建完整。
- **FR2**: ProfileLoader 启动时扫描 `.ibios/profiles/` 下所有 YAML 文件，用 SnakeYAML 解析为 Profile，并逐个校验（本节只校验 provider name 是否存在于全局 providers 列表（`ibios.providers`））。无效 Profile 记错误日志但不阻断启动。
- **FR3**: ProfileRegistry 将解析后的 Profile 存入内存索引（`Map<String, Profile>`），提供按名字查找。
- **FR4**: ProviderService 暴露单一方法 `ProviderResponse chat(sessionId, Profile, ProviderRequest)`。按 Profile 中的 provider name 从显式映射表查找对应 ChatModel，发起调用，返回 `ProviderResponse`。
- **FR5**: Provider 到 ChatModel 的映射**必须**显式构建（`Map<String, ChatModel>`），不走类型扫描。启动时按 `application.yaml` 的 `providers` 列表逐条构建。
- **FR6**: 每次 LLM 调用**必须**禁用 Spring AI 的自动工具执行（`autoExecuteTools=false`）。工具执行权完全归 IBIOS 的 ReActLoop + ToolExecutor。
- **FR7**: 工具格式适配器将 IBIOS 内部 `IbiosTool` 的参数描述翻译成 Spring AI 的工具描述格式。只翻译——输出中不含执行逻辑。
- **FR8**: LLM 调用成功时，将 provider、model、token 用量、延迟写入 `llm_calls` 表，`success=true`。
- **FR9**: LLM 调用失败时（超时、限流、模型报错），同样写入 `llm_calls`，`success=false`，`error_message` 记录原因，然后向上重新抛出异常。
- **FR10**: Profile 引用的 `provider.name` 若不在全局 providers 列表中，立即报错——不得静默用错 provider 或留空。
- **FR11**: 凭证使用环境变量占位符（`${DEEPSEEK_API_KEY}`）。代码和配置文件中不出现明文密钥。

## 4. 不在范围内（边界）

明确列出本节**不做**的事项。

- **Fallback**（一个 provider 挂了切另一个）— 扩展阶段
- **Hedge racing**（同时发多个 provider，取最快）— 扩展阶段
- **熔断** — 扩展阶段
- **费用看板**（只写 `llm_calls` 表，不提供聚合查询 API）— 扩展阶段

## 5. 验收标准

### 自动化（Harness）

`mvn test` / `mvn clean verify` 会跑的测试。关键回归点：

| # | 测试类 | 验证内容 |
|---|--------|---------|
| 1 | `ProfileLoaderTest` | 全字段 YAML 解析；未知 provider 清晰报错；坏文件不阻断其他加载；`${ENV}` 从环境变量解析 |
| 2 | `ProviderServiceTest` | 双 provider 路由不串台；未知 provider 抛异常；成功和失败都写审计；自动执行已禁用 |
| 3 | `ToolSchemaAdapterTest` | `IbiosTool` schema 翻译成 Spring AI 格式，字段逐一对齐；输出不含执行逻辑 |
| 4 | `LlmCallRepositoryTest` | `llm_calls` 表（手工建表脚本）支持读写；`success`/`error_message` 列物理存在 |
| 5 | `ProviderSmokeIT` | `@Tag("integration")` — 真实 API key 调真实模型；断言响应非空且 `llm_calls` 新增一条 `success=true` 记录 |

**三个关键回归测试方法**（在 `ProviderServiceTest` 中）：
1. **双 provider 路由不串台**：mock 两个 ChatModel，用 "kimi" profile 调用 → 验证 kimi 被调一次，deepseek 从未被调
2. **失败审计**：`chatModel.call()` 抛异常时，断言异常向上传播 **且** 审计记录 `success=false`，error_message 包含 "timeout"
3. **自动执行已禁用**：带 tools 调用 `chat()` 后，抓取 Request 对象 → 断言 `autoExecuteTools() == false` **且** `tools() != null`

### 手动

需人工验证的事项：

- 确认 Spring AI Alibaba starter 依赖在锁定 BOM 中存在且可解析（`mvn dependency:tree`）
- 用真实 API key 至少跑一次 `ProviderSmokeIT`；确认收到真实响应
- `grep -r "sk-"`（或你的 key 前缀）在代码和配置中找不到明文密钥
- 所有自动化测试点（双 provider 路由不串台、未知 provider 报错、成功/失败审计、自动执行禁用、只翻译不执行）均有单测覆盖 — `mvn test` 全绿 = 打勾

## 6. 依赖与假设

### 对前序课节的依赖

- 无（第 16 节是第一个编码课节）

### 外部依赖

- Spring AI Alibaba BOM：`spring-ai-alibaba-starter-dashscope` 必须可下载可解析（用 `mvn dependency:tree` 验证）
- 数据库：SQLite + Spring Data JPA；`llm_calls` 表需在现有 schema 上增加 `success` 和 `error_message` 列

---

## Constitution 合规检查

验证本 spec 符合 `.specify/memory/constitution.md`：

- [x] 原则 1：JDK 21 + Spring Boot 3.x 单体 — spec 基于此技术栈
- [x] 原则 2：五大核心能力优先 — Provider 是能力 #1
- [x] 原则 3：自研 ReAct 循环 — spec 明确说明 Provider 不执行工具
- [x] 原则 4：Spring AI 仅做协议，自动执行禁用 — FR6 + FR7 强制此约束；回归测试 #3
- [x] 原则 5：插件工具三层接入 — FR7 覆盖适配层
- [x] 原则 6：同步 + 虚拟线程 — spec 中无 reactive/async
- [x] 原则 7：Sandbox 接口先行 — 本节不适用（Sandbox 是第 24 节）
- [x] 原则 8：SQLite + 审计 Day-One — FR8/FR9 强制 llm_calls 审计；需更新 schema.sql
- [x] 原则 9：显式 Provider 映射 — FR5 强制显式 Map，不走类型扫描
- [x] 原则 10：全局不变量 — FR9 覆盖审计完整性；FR11 覆盖无明文密钥

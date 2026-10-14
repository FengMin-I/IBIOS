# 调研：Agent Provider 实现

## R1：不使用 Spring AI 自动配置如何创建 ChatModel bean

**决策**：手动工厂（`ProviderChatModelFactory`），读取 `ibios.providers` 配置，构造 `DashScopeChatModel` 实例。

**理由**：所有 DashScope 自动配置已排除。需要完全控制。工厂读配置，每个 provider 创建一个 ChatModel，返回 Map。

**已考虑的替代方案**：
- 重新启用 `DashScopeChatAutoConfiguration` — 否决：失去对 bean 命名和生命周期的控制
- 使用 `ChatClient.builder()` — 否决：ChatClient 是更高层抽象；ProviderService 需要原始 ChatModel

**关键 API**（Spring AI 1.0.0）：
- `DashScopeChatModel` 构造函数接受 `DashScopeApi`（后者接受 apiKey + baseUrl）
- `DashScopeApi` 在 `com.alibaba.cloud.ai.dashscope.api` 包中
- `ChatModel.call(ChatRequest)` 返回 `ChatResponse`
- `ChatRequest` builder 支持 `.tools(List<ToolCallback>)` 和 `.autoExecuteTools(false)`

## R2：SnakeYAML record 反序列化

**决策**：使用 SnakeYAML 的 `Constructor` 配合 `Profile.class`。Record 在 SnakeYAML 2.0+ 中可用（Spring Boot 3.x 自带 2.2+）。

**理由**：Record 提供不可变性。SnakeYAML 2.2+ 原生支持 record。

**注意事项**：Profile record 必须有规范构造函数。SnakeYAML 将 YAML key 与 record component 名匹配。

## R3：Spring AI ToolCallback / FunctionCallback API

**决策**：使用 Spring AI 的 `ToolCallback` 接口。`ToolSchemaAdapter` 将 `IbiosTool` 转换为 `ToolCallback`。

**理由**：`ChatRequest.tools()` 接受 `List<ToolCallback>`。这是 Spring AI 1.0.0 传递工具 schema 的标准 API。

**关键 API**：
- `ToolCallback` 有 `getToolDefinition()` 返回 `ToolDefinition`（name + description）
- `ToolDefinition` 有 `name()` 和 `description()`
- 输入 schema 来自 `IbiosTool.getInputSchema()`（JSON Schema 字符串）

## R4：SQLite ALTER TABLE 新增列

**决策**：使用 `ALTER TABLE llm_calls ADD COLUMN success INTEGER NOT NULL DEFAULT 1` 和 `ALTER TABLE llm_calls ADD COLUMN error_message TEXT`。

**理由**：SQLite 支持 ADD COLUMN。`NOT NULL DEFAULT` 是必须的，因为已有行需要有值。由于是新项目无生产数据，也可以直接重写 CREATE TABLE。

**选定方案**：直接重写 `schema.sql` 中的 CREATE TABLE，从一开始就包含所有列。对绿field项目更干净。

## R5：ChatRequest.autoExecuteTools() API 验证

**决策**：在 Spring AI 1.0.0 中，`ChatRequest` 本身**没有** `autoExecuteTools` 字段。自动执行是在 `ChatModel` 层面控制的，不是在 request 层面。

**理由**：Spring AI 的 `ChatModel.call(ChatRequest)` 在基础接口中**不会**自动执行工具。自动执行发生在 `ChatClient`（更高层 API）中，不在 `ChatModel` 中。由于我们直接使用 `ChatModel`，自动执行默认就是禁用的。

**含义**："自动执行已禁用" 的回归测试应验证我们使用的是 `ChatModel.call()`（不是 `ChatClient`），且请求包含工具 schema 但没有执行发生。测试断言 `autoExecuteTools() == false` 可能需要根据实际 Spring AI 1.0.0 API 调整。

**行动项**：实现阶段验证 ChatRequest/ChatResponse 的确切 Spring AI 1.0.0 API，相应调整测试断言。

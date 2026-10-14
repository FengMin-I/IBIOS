# 数据模型：Agent Provider

## 实体

### Profile

一个 record 类，表示 Agent 的运行时配置。

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `name` | String | YAML `name` | 唯一 Profile 标识 |
| `description` | String | YAML `description` | 可读描述 |
| `identity` | Identity | YAML `identity` | Agent 身份（agent_name, prompt） |
| `provider` | ProviderConfig | YAML `provider` | LLM provider 配置（name, model, temperature） |
| `tools` | List\<String\> | YAML `tools` | 可用工具名 |
| `skills` | List\<String\> | YAML `skills` | 绑定的技能名 |
| `mcpServers` | List\<String\> | YAML `mcp_servers` | MCP server 名 |
| `channels` | List\<String\> | YAML `channels` | 频道名 |
| `notifyChannels` | List\<String\> | YAML `notify_channels` | 通知频道名 |
| `schedules` | List\<ScheduleConfig\> | YAML `schedules` | 定时任务配置 |
| `bootstrap` | List\<String\> | YAML `bootstrap` | 引导文件路径 |
| `settings` | Map\<String, Object\> | YAML `settings` | 自由格式设置 |

**嵌套类型**：
- `Identity`：`agentName`（String）、`prompt`（String）
- `ProviderConfig`：`name`（String）、`model`（String）、`temperature`（Double）
- `ScheduleConfig`：`cron`（String）、`zone`（String）、`message`（String）

**校验规则**：
- `name` 必填且唯一
- `provider.name` 必须存在于全局 provider 注册表中（FR10）
- 无效 Profile 记错误日志但不阻断启动（FR2）

### LlmCall

LLM 调用审计记录的 JPA 实体。

| 列 | 类型 | 可空 | 说明 |
|----|------|------|------|
| `id` | Long（主键，自增） | 否 | 主键 |
| `session_id` | String | 是 | 会话标识 |
| `provider` | String | 否 | 使用的 Provider 名 |
| `model` | String | 否 | 使用的模型名 |
| `prompt_tokens` | Integer | 是 | 输入 token 数 |
| `completion_tokens` | Integer | 是 | 输出 token 数 |
| `total_tokens` | Integer | 是 | 总 token 数 |
| `latency_ms` | Long | 是 | 调用耗时（毫秒） |
| `success` | Boolean | 否 | 调用成功标志 |
| `error_message` | String | 是 | 失败时的错误信息 |
| `called_at` | Instant | 否 | 时间戳 |

**Schema**（schema.sql）：
```sql
CREATE TABLE IF NOT EXISTS llm_calls (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id        TEXT,
    provider          TEXT    NOT NULL,
    model             TEXT    NOT NULL,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    total_tokens      INTEGER,
    latency_ms        INTEGER,
    success           INTEGER NOT NULL DEFAULT 1,
    error_message     TEXT,
    called_at         TEXT    NOT NULL
);
```

### ProviderConfig（application.yaml）

全局 provider 配置。

```yaml
ibios:
  providers:
    - name: dashscope
      api-key: ${DASHSCOPE_API_KEY:}
      model: qwen-plus
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 唯一 Provider 标识 |
| `api-key` | String | API key（环境变量占位） |
| `model` | String | 默认模型名 |

## 关系

- `Profile.provider.name` → 引用 `ibios.providers` 列表中的某个 name（FR10）
- `LlmCall` 由 `ProviderService` 在每次 LLM 调用时写入（FR8/FR9）
- `ProfileRegistry` 持有 `Map<String, Profile>`，按 `Profile.name` 索引
- `ProviderChatModelFactory` 持有 `Map<String, ChatModel>`，按 provider name 索引

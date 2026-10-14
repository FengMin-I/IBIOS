# Data Model: ReAct 循环引擎

## Entities

### ToolInvocation (新增)

每次工具调用的审计记录。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (主键，自增) | 唯一标识 |
| `session_id` | String (非空) | 关联 Session，用于审计追溯 |
| `tool_name` | String (非空) | 被调用的工具名称 |
| `input_json` | String (可空) | 工具输入参数（JSON 序列化） |
| `output_json` | String (可空) | 工具输出结果（JSON 序列化） |
| `success` | boolean (非空) | 是否成功 |
| `error_message` | String (可空) | 失败时的错误信息 |
| `duration_ms` | Long (可空) | 执行耗时（毫秒） |
| `created_at` | Instant (非空) | 创建时间 |

**关系**: 多对一关联 Session（通过 session_id）

**状态转换**: 无（创建即完成，success 字段标记结果）

### Session (已有，Lesson 16)

对话会话，包含对话历史。

| 字段 | 说明 |
|------|------|
| `session_id` | 主键 |
| `profile_name` | 关联 Profile |
| `messages_json` | JSON 序列化的对话历史 |
| `status` | active / archived |

### Profile (已有，Lesson 16)

Agent 运行配置。

| 字段 | 说明 |
|------|------|
| `name` | Profile 名称 |
| `provider` | LLM Provider 名称 |
| `model` | 模型名称 |
| `maxIterations` | 最大迭代次数（默认 10） |
| `maxHistoryTurns` | 历史截断轮数（默认 20） |
| `bootstrap` | Bootstrap 文件路径列表 |
| `skills` | Skill 绑定列表 |

## Validation Rules

- `ToolInvocation.session_id` 必须非空
- `ToolInvocation.tool_name` 必须非空
- `ToolInvocation.success` 必须明确设置（true 或 false）
- `Profile.maxIterations` 必须 > 0
- `Profile.maxHistoryTurns` 必须 > 0

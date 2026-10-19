# Data Model: Agent Lifecycle Management

## 实体

本节不新增数据库表。所有持久化沿用前序节的表结构。

### AgentView（DTO，非持久化）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | Agent 名称（目录名） |
| description | String | 描述 |
| provider | String | Provider 名称 |
| model | String | 模型名称 |
| tools | List<String> | 工具列表 |
| schedules | List<ScheduleSummary> | 定时配置 |
| agentDir | String | Agent 目录绝对路径 |
| hasScripts | boolean | 是否有 scripts/ |
| hasSkills | boolean | 是否有 skills/ |
| hasReference | boolean | 是否有 REFERENCE.md |
| hasMemory | boolean | 是否有 MEMORY.md |

### FileNode（DTO，目录树节点）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 文件/目录名 |
| path | String | 相对路径（相对于 .ibios/） |
| type | String | "file" 或 "directory" |
| children | List<FileNode> | 子节点（目录时非空） |

### SessionView（DTO）

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话 ID |
| profileName | String | Agent 名称 |
| messages | List<MessageView> | 最近 ≤100 条消息 |

## 目录结构

```
.ibios/
├── agents/           # Agent 目录（唯一真相源）
│   └── <name>/
│       ├── AGENT.md
│       ├── MEMORY.md  # per-agent 记忆（可选）
│       ├── scripts/   # 可选
│       ├── skills/    # 可选（软连接）
│       └── REFERENCE.md  # 可选
└── archive/          # 删除归档（不物理删）
    └── <name>/
```

## 配置新增

| 键 | 类型 | 说明 |
|---|------|------|
| `ibios.author.provider` | String | 生成用 provider（缺省取 providers 第一个） |
| `ibios.author.model` | String | 生成用 model（留空 → generate 端点 503） |

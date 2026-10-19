# REST API Contracts: Agent Lifecycle

## Agent CRUD

### POST /api/v1/agents — 创建 Agent

**Request**:
```json
{
  "name": "weather-daily",
  "description": "每天早上查天气推送团队群"
}
```

**Response** (200):
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "name": "weather-daily",
    "description": "每天早上查天气推送团队群",
    "provider": "dashscope",
    "model": "qwen-plus",
    "tools": [],
    "schedules": [],
    "agentDir": "/path/to/.ibios/agents/weather-daily",
    "hasScripts": false,
    "hasSkills": false,
    "hasReference": true,
    "hasMemory": false
  }
}
```

**Error** (400): name 已存在 / name 非法
```json
{ "code": 40001, "message": "Agent 已存在: weather-daily", "data": null }
```

### GET /api/v1/agents — 列表

**Response** (200):
```json
{
  "code": 0,
  "message": "ok",
  "data": [ { "name": "...", ... }, ... ]
}
```

### GET /api/v1/agents/{name} — 详情

**Response** (200): 同 create 的 data 结构
**Error** (404): Agent 不存在

### PUT /api/v1/agents/{name} — 更新

**Request**:
```json
{
  "markdown": "---\nname: weather-daily\nprovider: dashscope\n..."
}
```

**Response** (200): 更新后的 AgentView
**Error** (404): Agent 不存在

### DELETE /api/v1/agents/{name} — 删除

**Response** (200): `{ "code": 0, "message": "ok", "data": null }`
**Error** (404): Agent 不存在

### POST /api/v1/agents/{name}/invoke — 无状态调用（26 节已有，不变）

## 一句话生成与文件管理

### POST /api/v1/agents/{name}/generate-files — LLM 生成文件草稿

**Request**:
```json
{ "description": "每天早上九点查北京天气，把穿搭建议发到团队群" }
```

**Response** (200):
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "AGENT.md": "---\nname: weather-daily\n...",
    "scripts/reconcile.py": "...",
    "REFERENCE.md": "..."
  }
}
```

**Error** (400): LLM 产出非法 AGENT.md
**Error** (503): 生成用 model 未配置

### POST /api/v1/agents/{name}/files — 保存一组文件

**Request**:
```json
{
  "files": {
    "AGENT.md": "---\n...",
    "scripts/reconcile.py": "..."
  }
}
```

**Response** (200): 更新后的 AgentView
**Error** (400): AGENT.md 不可解析

## Per-Agent 记忆与会话

### GET /api/v1/agents/{name}/memory — Agent 专属记忆

**Response** (200):
```json
{ "code": 0, "message": "ok", "data": { "content": "# Memory\n..." } }
```

### GET /api/v1/agents/{name}/session — 固定管理会话

**Response** (200):
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "sessionId": "abc123",
    "profileName": "weather-daily",
    "messages": [ ... ]
  }
}
```

### POST /api/v1/agents/{name}/session/messages — 发消息

**Request**:
```json
{ "content": "今天天气怎么样？" }
```

**Response** (200): Agent 回复文本

## 工作区文件浏览

### GET /api/v1/workspace/tree — 目录树

**Response** (200):
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "name": ".ibios",
    "type": "directory",
    "children": [
      {
        "name": "agents",
        "type": "directory",
        "children": [
          {
            "name": "weather-daily",
            "type": "directory",
            "children": [
              { "name": "AGENT.md", "type": "file", "path": "agents/weather-daily/AGENT.md" },
              { "name": "scripts", "type": "directory", "children": [...] }
            ]
          }
        ]
      },
      { "name": "archive", "type": "directory", "children": [] }
    ]
  }
}
```

### GET /api/v1/workspace/file?path=agents/weather-daily/AGENT.md — 读文件

**Response** (200): `{ "code": 0, "message": "ok", "data": { "path": "...", "content": "..." } }`
**Error** (400): 目录穿越 `path=../../etc/passwd`

### POST /api/v1/workspace/file — 写文件

**Request**:
```json
{ "path": "agents/weather-daily/AGENT.md", "content": "---\n..." }
```

**Response** (200): ok
**Error** (400): 目录穿越

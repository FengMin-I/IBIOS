# Data Model: Memory 三层记忆

## 实体：MemoryEntry（SQLite 后端专用）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 自增主键 |
| scope | VARCHAR(16) | NOT NULL | `CORE` 或 `ARCHIVAL` |
| content | TEXT | NOT NULL | 记忆内容 |
| created_at | TEXT | NOT NULL | ISO-8601 时间戳 |

**索引**: `idx_memory_scope ON memory_entries (scope)` —— 按 scope 查询是高频操作。

**建表位置**: `ibios-boot/src/main/resources/schema.sql` 追加，与 sessions/tool_invocations/llm_calls 同口径。

## 枚举：MemoryScope

| 值 | 语义 | 截断 | 检索 |
|----|------|------|------|
| CORE | 核心记忆，始终在场 | 永不截断 | 不参与检索（全量注入） |
| ARCHIVAL | 归档记忆 | 可截断（md: 4000字符 / sqlite: LIMIT 100） | 关键词检索区 |

## 接口关系

```
PromptBuilder ──→ MemoryService（接口，ibios-core）
                      │
                      ├── buildContext(session) → String
                      ├── remember(content, scope)
                      └── recall(keyword) → List<String>
                      │
                      └──→ LongTermMemoryStore（接口，ibios-core）
                              │
                              ├── append(content, scope)
                              ├── load() → String
                              └── recallByKeyword(keyword) → List<String>
                              │
                              ├── MarkdownMemoryStore（ibios-memory）
                              ├── SqliteMemoryStore（ibios-memory）
                              └── Mem0MemoryStore（ibios-memory）

MemoryTools ──→ MemoryService（同上，对后端无感）
```

## 文件约定

- Markdown 后端操作文件：`.ibios/memory/MEMORY.md`
- 文件内两个分区 header：`## 核心记忆` / `## 归档记忆`
- 每条记忆格式：`- [YYYY-MM-DD] 内容`

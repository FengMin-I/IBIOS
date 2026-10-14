# 第 18 节技术方案：CLI 命令行入口

## 技术栈

- JDK 21 + Spring Boot 3.x + Picocli
- SQLite + Spring Data JPA
- JSON 序列化：Jackson（Spring Boot 自带）

## 模块落位

| 模块 | 交付物 |
|------|--------|
| ibios-core | `SessionManager` 接口（扩展已有内部接口为独立接口，含 `getOrCreate`/`get`/`save`） |
| ibios-storage | `Session` JPA 实体、`SessionRepository`、`JpaSessionManager`（实现 SessionManager） |
| ibios-cli | `IbiosOsCli` 主入口 + 12 个 `@Command` 子命令类 |
| ibios-channel-cli | `CliChannel`（chat 命令的交互逻辑） |
| ibios-boot | `schema.sql` 追加 `sessions` 表建表语句 |

## 数据模型

### sessions 表

```sql
CREATE TABLE IF NOT EXISTS sessions (
    session_id      TEXT PRIMARY KEY,
    profile_name    TEXT    NOT NULL,
    channel         TEXT    NOT NULL,
    user_id         TEXT    NOT NULL,
    messages_json   TEXT,
    status          TEXT    NOT NULL DEFAULT 'active',
    created_at      TEXT    NOT NULL,
    last_active_at  TEXT    NOT NULL,
    archived_at     TEXT
);
```

### Session JPA 实体

```
SessionEntity
├── sessionId: String (PK)
├── profileName: String
├── channel: String
├── userId: String
├── messagesJson: String
├── status: String
├── createdAt: String
├── lastActiveAt: String
└── archivedAt: String (nullable)
```

### session_id 生成公式

```
session_id = channel + ":" + user + ":" + profileName
```

拼接只发生在 `SessionManager.getOrCreate` 内部，调用方只提供三元组。

## 架构设计

### 命令轻重分流

```
IbiosOsCli (main)
├── 轻命令（不起 Spring）
│   ├── init
│   ├── profile list/create/show/delete
│   ├── provider list
│   ├── tool list
│   └── session list
└── 重命令（起 Spring）
    ├── chat → CliChannel
    ├── serve（骨架，26 节实现）
    └── gateway（骨架，后续节实现）
```

轻命令直接做文件操作或查状态，秒回。重命令启动 Spring Boot 上下文后再执行。

### 重命令启动类

```
IbiosSpringApp
@SpringBootApplication(scanBasePackages = {"com.ibios.core", "com.ibios.storage", "com.ibios.provider", "com.ibios.channel.cli"})
@EnableJpaRepositories(basePackages = "com.ibios.storage")
@EntityScan(basePackages = "com.ibios.storage")
```

### CliChannel 交互流程

```
chat --profile <name>
    ↓
SessionManager.getOrCreate("cli", currentUser(), profileName)
    ↓
while (true):
    读 stdin → "/quit"? → break
    → AgentService.process(session, line)
    → println(reply)
```

### SessionManager 接口（ibios-core）

```
interface SessionManager {
    Session getOrCreate(String channel, String userId, String profileName);
    Session get(String sessionId);
    void save(Session session);
}
```

### JpaSessionManager（ibios-storage）

- `getOrCreate`: 按 session_id 查找，不存在则新建
- `get`: 按 session_id 查找，转成 core Session
- `save`: 将 core Session 序列化为 JSON 存入 messages_json

### Session 域模型（ibios-core）扩展

现有 `Session` 类需要扩展：
- 新增 `channel` 和 `userId` 字段
- `messages` 支持 JSON 序列化/反序列化

## 测试策略

| 测试类 | 模块 | 覆盖点 |
|--------|------|--------|
| SessionManagerTest | ibios-storage | 幂等性、隔离性、id 生成唯一处 |
| SessionRepositoryTest | ibios-storage | 建表脚本能存能读、JSON 序列化完整、重启不丢 |

单测默认跑，集成冒烟打 `@Tag("integration")` CI 跳过。

## 语法禁区

- 避开 P3C/ASM 解析不了的 Java 18+ 语法（如增强 switch 的 `default ->` 写法）
- 测试方法名用英文，课件中文方法名用 `@DisplayName` 保留原文

# 接口契约: Memory 模块跨模块接口

## 1. MemoryService（ibios-core → ibios-memory 实现）

```java
package com.ibios.core.memory;

public interface MemoryService {
    /**
     * 构建记忆上下文，供 PromptBuilder 拼入 system prompt。
     * 返回内容包含核心记忆（全量）+ 归档记忆（截断后）。
     */
    String buildContext(Session session);

    /**
     * 写入一条长期记忆。
     * @param content 记忆内容
     * @param scope   CORE 或 ARCHIVAL（缺省 ARCHIVAL）
     */
    void remember(String content, MemoryScope scope);

    /**
     * 按关键词检索归档记忆区。核心记忆不参与检索（全量注入）。
     * @return 匹配的记忆条目列表，未命中返回空列表
     */
    List<String> recall(String keyword);
}
```

**调用方**: `PromptBuilder.build(session, profile)` 内部调用 `buildContext(session)`；`MemoryTools` 调用 `remember` 和 `recall`。

**实现方**: `MemoryServiceImpl`（ibios-memory），注入 `LongTermMemoryStore` + `SessionManager`。

## 2. LongTermMemoryStore（ibios-core 接口 → ibios-memory 三档实现）

```java
package com.ibios.core.memory;

public interface LongTermMemoryStore {
    void append(String content, MemoryScope scope);
    String load();                        // 核心全量 + 归档截断后
    List<String> recallByKeyword(String keyword);  // 只搜归档区
}
```

**三档实现**:
- `MarkdownMemoryStore` — 默认，操作 `.ibios/memory/MEMORY.md`
- `SqliteMemoryStore` — 操作 `memory_entries` 表
- `Mem0MemoryStore` — REST 调用自托管 Mem0 server

**四条行为契约**:
1. 不缓存（每次重新读）
2. 核心记忆永不被截断
3. scope 由调用方显式指定
4. recall 是简单关键词匹配

## 3. MemoryScope（ibios-core 枚举）

```java
package com.ibios.core.memory;

public enum MemoryScope {
    CORE,
    ARCHIVAL
}
```

## 4. MemoryTools → ToolRegistry 注册契约

`MemoryTools` 类提供两个 `@Tool` 方法，包装成 `IbiosTool` 注册到 `ToolRegistry`：

| Tool 名 | 参数 | 返回值 | 行为 |
|---------|------|--------|------|
| `save_memory` | content: String, scope: String（缺省 "archival"） | "已记住" | 调用 `memoryService.remember()` |
| `recall_memory` | keyword: String | 匹配结果拼接或"没有找到相关记忆" | 调用 `memoryService.recall()` |

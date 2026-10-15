# Research: Memory 三层记忆

## R1: ibios-memory 模块现有依赖是否满足需求

**Decision**: 现有 pom.xml 依赖已满足，无需新增。

**Rationale**: ibios-memory 已声明 ibios-core、spring-context、spring-boot-starter-data-jpa、sqlite-jdbc、hibernate-community-dialects 五个依赖。MarkdownMemoryStore 只需 JDK 自带的 java.nio.file；SqliteMemoryStore 复用 spring-data-jpa + sqlite-jdbc；Mem0MemoryStore 需要 HTTP 调用，spring-context 传递依赖了 spring-web 的 RestClient（Spring 6.1+），无需额外引入。

**Alternatives considered**: 为 Mem0 引入 OkHttp/WebClient——增加依赖重量，RestClient 已够用。

## R2: MemoryService 接口放 ibios-core 还是 ibios-memory

**Decision**: 接口（MemoryService、MemoryScope、LongTermMemoryStore）放 ibios-core，实现放 ibios-memory。

**Rationale**: PromptBuilder（ibios-core）需要在 `build()` 方法中调用 `memoryService.buildContext(session)`。如果 MemoryService 接口在 ibios-memory，ibios-core 就要反向依赖 ibios-memory，破坏依赖方向。接口在 core、实现在 memory，依赖方向 memory → core 单向。

**Alternatives considered**: 全放 ibios-memory，PromptBuilder 通过事件/回调解耦——过度设计，违背"接口先行"原则。

## R3: Mem0 REST API 格式

**Decision**: 契约测试用内存替身覆盖行为契约；`Mem0MemoryStoreTest` 用 MockWebServer 验证请求体格式。真实 Mem0 API 格式以部署版本为准，不在核心阶段锁定。

**Rationale**: Mem0 是外部服务，核心阶段不绑定具体版本。内存替身（`InMemoryMem0Stub`）实现 `LongTermMemoryStore` 接口，行为满足四条契约即可参与参数化契约测试。

**Alternatives considered**: 直接 mock RestClient（Mockito）——可行但无法参与参数化契约测试（契约测试需要真实实现实例），改用内存替身 + 单独 mock 测试 REST 交互。

## R4: memory_entries 表结构

**Decision**: 与课件一致，四列：id（自增主键）、scope（VARCHAR(16)）、content（TEXT）、created_at（TEXT）。SQLite 用 TEXT 存时间戳（ISO-8601），与 sessions/llm_calls 同口径。

**Rationale**: 宪法八要求手工建表、不依赖 ddl-auto。与现有表风格一致（时间用 TEXT 而非 TIMESTAMP）。

**Alternatives considered**: 加 updated_at 列——核心阶段不做更新（append-only），不需要。

# Research: ReAct 循环引擎

## Decisions

### Decision 1: Spring AI Alibaba BOM 依赖确认

- **Decision**: 使用 `mvn dependency:tree` 确认 BOM 中 Spring AI 相关依赖存在
- **Rationale**: 课件要求动手前确认锁定 BOM 里目标依赖存在
- **Alternatives considered**: 直接编码后编译失败再调整

### Decision 2: ToolInvocation 审计表字段设计

- **Decision**: 与 llm_calls 表保持相同口径：session_id、名称、输入/输出 JSON、success、error_message、duration_ms、created_at
- **Rationale**: 宪法原则八要求审计 day-one，统一口径便于后续查询
- **Alternatives considered**: 只记成功；用不同字段命名

### Decision 3: ProfileContext ThreadLocal 清除策略

- **Decision**: AgentService.process 的 finally 块中清除 ProfileContext
- **Rationale**: 虚拟线程下每请求独立，但 finally 清除防止异常时泄漏
- **Alternatives considered**: try-with-resources 包装；AOP 切面

### Decision 4: ContextLoader 文件读取策略

- **Decision**: 每次组装 prompt 时重新读文件，不缓存
- **Rationale**: 课件明确要求，确保用户修改后立即生效
- **Alternatives considered**: 启动时加载缓存；文件变更监听

## Status

All NEEDS CLARIFICATION items resolved. No blocking unknowns.

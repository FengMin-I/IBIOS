# Research: 插件化 Agent 技术决策

## R1: AGENT.md frontmatter 解析策略

**决策**：用 SnakeYAML 解析 frontmatter。按 `---` 分隔符拆分：第一个 `---` 到第二个 `---` 之间是 YAML frontmatter，之后是正文。

**理由**：SnakeYAML 已在 ibios-core 依赖中（ProfileLoader 使用）。`---` 分隔符是 Markdown frontmatter 的事实标准（Jekyll、Hugo、VuePress 均用此格式）。

**替代方案**：
- 用正则匹配 `\A---\n(.*?)\n---\n(.*)\z`：简单但不够健壮（多行 YAML 可能有问题）
- 用专门的 frontmatter 库：引入新依赖，不值得

## R2: Profile record 扩展方式

**决策**：在 Profile record 末尾新增 `Path agentDir` 字段。

**理由**：
- Record 的 compact constructor 已有默认值逻辑（maxIterations/maxHistoryTurns）
- 新增字段放末尾，现有调用方只需补 null
- 手写 Profile YAML 来源的 Profile，agentDir = null，不影响既有行为

**替代方案**：
- 新建 `AgentProfile extends Profile`：Record 不能继承，且底座一切吃 Profile 类型，子类需要大量 cast
- 用 `Map<String, Path> agentDirByProfileName` 外挂：ContextLoader 需要额外注入，生命周期复杂

## R3: AgentScheduler.registerProfile 的幂等性

**决策**：本节不要求幂等。重复调用会注册两次定时。第 30 节在调用前先 unregister。

**理由**：本节只做"启动扫描 + 运行时注册"，不涉及更新。第 30 节的 PUT /api/v1/agents/{name} 需要先 unregister 再 register，那时再加幂等。

## R4: 示例 Agent 目录的脚本语言

**决策**：示例 `daily-reconcile/scripts/reconcile.py` 用 Python 标准库（csv、json、os、sys、datetime），无外部依赖。

**理由**：课件 §1.4 已给出完整代码。Python 标准库足够，不需要 pip install。沙箱只需放行 `python` 解释器 + Agent 自己的 `scripts/` 目录。

## R5: ContextLoader.loadAgentBody 的缓存策略

**决策**：不缓存。每次调用都现读 AGENT.md。

**理由**：课件明确要求"改 Agent 目录里的正文不重启，下一次触发就用新说明"。这与 ContextLoader 既有行为一致（loadBootstrapContent 也是每次现读）。17 节 ContextLoaderTest 的无缓存回归已钉死。

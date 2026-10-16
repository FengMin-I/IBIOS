# AgentLoader 契约

## 职责

解析 `.ibios/agents/<name>/` 目录，拆出 frontmatter（YAML 配置）、正文（任务指令）和资源路径。

## 方法契约

### `AgentDefinition load(Path agentDir)`

**前置条件**：
- `agentDir` 存在且是目录
- `agentDir/AGENT.md` 存在

**行为**：
1. 读 `AGENT.md` 全文
2. 按 `---` 分隔符拆 frontmatter 和正文（第一个 `---` 到第二个 `---` 之间是 frontmatter，之后是正文）
3. 检查 `scripts/`、`skills/`、`REFERENCE.md` 是否存在，记录路径
4. 返回 `AgentDefinition`

**异常**：
- `IllegalArgumentException`：`AGENT.md` 不存在
- `IllegalArgumentException`：frontmatter 缺少 `name` 字段
- `IllegalArgumentException`：frontmatter 缺少 `provider` 字段（含 `provider.name`）

### `Profile deriveProfile(Path agentDir)`

**行为**：
1. 调 `load(agentDir)` 得到 `AgentDefinition`
2. 把 frontmatter 各字段映射到 `Profile` record
3. `Profile.agentDir` 设为 `agentDir.toAbsolutePath()`
4. `Profile.name` 默认取 frontmatter 的 `name`，若缺省则取目录名
5. 返回 `Profile`

### `void scanAndRegister(Path agentsDir, ProfileRegistry registry, AgentScheduler scheduler, Set<String> validProviderNames, Set<String> registeredToolNames)`

**行为**：
1. 若 `agentsDir` 不存在，LOG.info 并返回
2. 遍历 `agentsDir` 下所有子目录
3. 对每个子目录调 `deriveProfile`
4. 校验 `provider.name` 在 `validProviderNames` 中，否则抛异常
5. 检查 `tools` 列表中每项是否在 `registeredToolNames` 中，不在的 LOG.warning
6. `registry.register(profile)`
7. 若 `scheduler` 非 null，调 `scheduler.registerProfile(profile)`

## 测试锚点

- `AgentLoaderTest`：frontmatter/正文拆分正确、资源路径识别、缺必填字段报错
- `DeriveProfileTest`：字段映射正确、agentDir 设置、schedules 原样带入

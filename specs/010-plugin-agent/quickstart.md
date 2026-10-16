# Quickstart: 插件化 Agent 验证指南

## 前置条件

- JDK 21 + Maven 3.9+
- 环境变量 `DASHSCOPE_API_KEY` 已设置（真模型链路）或 Mock Provider 已配置

## 验证场景

### 场景 1：AgentLoader 解析 AGENT.md

```bash
# 创建临时测试目录
mkdir -p /tmp/test-agent/my-agent
cat > /tmp/test-agent/my-agent/AGENT.md << 'EOF'
---
name: my-agent
provider: dashscope
model: qwen-plus
tools: [http_get, read_file]
schedules:
  - {id: test-cron, cron: "0 0 9 * * *", zone: Asia/Shanghai, message: hello}
---

# 我的 Agent

你是一个测试助手。
EOF

# 单测验证
mvn test -pl ibios-core -Dtest=AgentLoaderTest
```

**预期**：AgentLoaderTest 全绿，frontmatter 正确拆出、正文正确提取。

### 场景 2：deriveProfile 映射

```bash
mvn test -pl ibios-core -Dtest=DeriveProfileTest
```

**预期**：DeriveProfileTest 全绿，Profile 各字段映射正确、agentDir 非空、schedules 原样带入。

### 场景 3：扫描注册

```bash
mvn test -pl ibios-core -Dtest=AgentScanRegisterTest
```

**预期**：放 N 个目录 → Registry 出现 N 个 Agent。

### 场景 4：运行时注册

```bash
mvn test -pl ibios-core -Dtest=ProfileRegistryRuntimeTest
```

**预期**：register 后可见、remove 后不可见、exists 正确。

### 场景 5：定时句柄

```bash
mvn test -pl ibios-core -Dtest=AgentSchedulerRegisterTest
```

**预期**：registerProfile 后 scheduledTasks 有句柄。

### 场景 6：渐进式披露

```bash
mvn test -pl ibios-core -Dtest=ProgressiveDisclosureTest
```

**预期**：Agent 正文进 system prompt、参考/脚本不预载。

### 全量验证

```bash
mvn clean verify
```

**预期**：全绿。

## 人工验证（需真模型）

1. 往 `.ibios/agents/` 放一个 Agent 目录
2. 启动系统：`java -jar ibios-boot/target/ibios-boot-0.0.1-SNAPSHOT.jar serve`
3. 检查 `GET http://localhost:8080/api/v1/profiles` 里出现新 Agent
4. 改 AGENT.md 正文，不重启，再触发一次，验证新说明生效

# Quickstart: ReAct 循环引擎验证

## 前置条件

- JDK 21+
- Maven 3.9+
- Lesson 16 已完成（Profile/ProviderService/LlmCall 已实现）

## 验证场景

### 场景 1: 单元测试全绿

```bash
# 编译 + 跑单测
mvn clean verify

# 预期：BUILD SUCCESS，所有测试通过
```

### 场景 2: 只跑本节测试

```bash
# 只跑 ibios-core 的测试
mvn test -pl ibios-core

# 只跑 ibios-storage 的测试
mvn test -pl ibios-storage

# 预期：ReActLoopTest、PromptBuilderTest、ToolExecutorTest、AgentServiceTest、ContextLoaderTest 全绿
```

### 场景 3: 关键回归测试

```bash
# 死循环兜底测试
mvn test -pl ibios-core -Dtest=ReActLoopTest#模型一直要调工具_转满最大轮数强制停

# ThreadLocal 泄漏测试
mvn test -pl ibios-core -Dtest=AgentServiceTest#处理中抛异常_ProfileContext也必须被清掉

# 预期：两个测试都通过
```

### 场景 4: 依赖方向检查

```bash
# 确认 ibios-core 不依赖 Spring 框架（只有 SnakeYAML）
grep -r "org.springframework" ibios-core/src/main/java/ || echo "PASS: core 无 Spring 依赖"

# 确认 ibios-provider 依赖 ibios-core
grep "ibios-core" ibios-provider/pom.xml

# 确认 ibios-storage 依赖 ibios-core
grep "ibios-core" ibios-storage/pom.xml
```

### 场景 5: 审计表存在性检查

```bash
# 确认 tool_invocations 建表脚本存在
grep "tool_invocations" ibios-boot/src/main/resources/schema.sql

# 确认 ToolInvocation 实体存在
ls ibios-storage/src/main/java/com/ibios/storage/audit/ToolInvocation.java

# 确认 ToolInvocationRepository 存在
ls ibios-storage/src/main/java/com/ibios/storage/audit/ToolInvocationRepository.java
```

## 预期结果

- `mvn clean verify` 全绿（含 P3C/SpotBugs/FindSecBugs/PMD）
- 5 个测试类全部存在且非空
- 关键回归测试通过
- 依赖方向正确
- 审计表脚本和实体存在

## 人工验证（后续）

- Demo 一（每日天气）用真模型跑通：问天气 → Agent 调 http_get → 拿到数据 → 给出穿搭建议
- Code review 确认循环是自实现的 ReActLoop，未使用 Spring AI 的 Agent/AgentExecutor 抽象

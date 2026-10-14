# 快速验证指南：Agent Provider

## 前置条件

- 已安装 JDK 21
- 已安装 Maven 3.9+
- 已设置 `DASHSCOPE_API_KEY` 环境变量（仅冒烟测试需要）

## 验证场景

### 场景 1：单测通过

```bash
mvn test -pl ibios-core,ibios-provider,ibios-storage
```

**预期**：全部测试通过。关键断言：
- `ProviderServiceTest.routeByProviderName_twoProvidersNoCrossover` — kimi 被调用，deepseek 从未被调用
- `ProviderServiceTest.callFailure_auditRecordsSuccessFalse` — 异常抛出且审计已记录
- `ProviderServiceTest.callWithTools_autoExecuteDisabled` — 工具存在，自动执行已禁用
- `ProfileLoaderTest.validYaml_allFieldsParsed` — 所有 Profile 字段已填充
- `ProfileLoaderTest.unknownProvider_clearError` — 清晰错误信息
- `ProfileLoaderTest.badFile_doesNotBlockOthers` — 其他 Profile 仍然加载
- `ProfileLoaderTest.envPlaceholder_resolvedFromEnvironment` — ${ENV} 已解析
- `ToolSchemaAdapterTest.schemaTranslation_fieldsAligned` — 字段逐一对齐
- `LlmCallRepositoryTest.tableSupportsReadWrite` — CRUD 可用，success/error_message 列存在

### 场景 2：全量构建通过

```bash
mvn clean verify
```

**预期**：所有模块编译通过，所有测试通过，所有静态检查门禁通过（Spotless、Checkstyle、SpotBugs、PMD/P3C、FindSecBugs）。

### 场景 3：应用启动

```bash
mvn -pl ibios-boot spring-boot:run
```

**预期**：应用无错误启动。ProfileLoader 扫描 `.ibios/profiles/`（空目录也可以）。ProviderChatModelFactory 从 `ibios.providers` 配置创建 ChatModel bean。

### 场景 4：冒烟测试（需要真实 API key）

```bash
DASHSCOPE_API_KEY=your-key mvn test -Dgroups=integration -pl ibios-provider
```

**预期**：`ProviderSmokeIT` 通过 — 真实 LLM 调用返回非空响应，`llm_calls` 表新增一条 `success=true` 记录。

### 场景 5：无明文密钥

```bash
grep -rn "sk-\|dashscope-[a-zA-Z0-9]\{20,\}" --include="*.java" --include="*.yaml" --include="*.yml" --include="*.properties" .
```

**预期**：无匹配结果（测试资源中使用显式测试占位符的除外）。

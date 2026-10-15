# Quickstart: Memory 模块验证指南

## 前置条件

- JDK 21、Maven
- 前序模块已编译通过（`mvn clean verify` 全绿）

## 验证一：契约测试覆盖三档后端

```bash
# 跑 Memory 模块全部测试
mvn test -pl ibios-memory

# 只跑契约测试（参数化覆盖三档实现）
mvn test -pl ibios-memory -Dtest=MemoryStoreContractTest
```

**预期**: 全部绿。契约测试的每个 `@ParameterizedTest` 应显示 3 个 invocation（markdown / sqlite / mem0-stub）。

## 验证二：关键回归点

```bash
# 截断只裁归档（契约二）
mvn test -pl ibios-memory -Dtest="MemoryStoreContractTest#truncationOnlyAffectsArchival*"

# 写后立读（契约一）
mvn test -pl ibios-memory -Dtest="MemoryStoreContractTest#writeThenReadImmediately*"
```

**预期**: 每个测试 3 个 invocation 全绿。

## 验证三：全量门禁

```bash
mvn clean verify
```

**预期**: 全绿（含 P3C / SpotBugs / FindSecBugs / PMD）。

## 验证四：配置切换（人工）

1. `application.yaml` 设 `memory.backend: markdown`，启动对话，调 `save_memory` 写入，检查 `.ibios/memory/MEMORY.md` 文件内容
2. 改为 `memory.backend: sqlite`，重复同样操作，检查 `memory_entries` 表
3. 两次对话体感一致，底层后端不同

# 任务清单：CLI 命令行入口

## Phase 1: Schema + 持久化实体

- [X] T001 在 `ibios-boot/src/main/resources/schema.sql` 追加 `sessions` 表建表语句
- [X] T002 [P] 创建 `ibios-storage/src/main/java/com/ibios/storage/session/SessionEntity.java` JPA 实体
- [X] T003 [P] 创建 `ibios-storage/src/main/java/com/ibios/storage/session/SessionRepository.java`

## Phase 2: Core Session 扩展 + SessionManager

- [X] T004 扩展 `ibios-core/src/main/java/com/ibios/core/session/Session.java`，新增 `channel`/`userId` 字段
- [X] T005 将 `AgentService.SessionManager` 内部接口提取为独立接口 `ibios-core/src/main/java/com/ibios/core/session/SessionManager.java`，新增 `getOrCreate`/`get` 方法
- [X] T006 更新 `ibios-core/src/main/java/com/ibios/core/react/AgentService.java`，使用独立 SessionManager 接口
- [X] T007 创建 `ibios-storage/src/main/java/com/ibios/storage/session/JpaSessionManager.java`，实现 SessionManager 接口

## Phase 3: 验收 harness 测试（先于实现）

- [X] T008 创建 `ibios-storage/src/test/java/com/ibios/storage/session/SessionManagerTest.java`（幂等、隔离、id 唯一处）
- [X] T009 [P] 创建 `ibios-storage/src/test/java/com/ibios/storage/session/SessionRepositoryTest.java`（建表能存能读、JSON 完整、重启不丢）

## Phase 4: CLI 命令

- [X] T010 创建 `ibios-cli/src/main/java/com/ibios/cli/IbiosOsCli.java` 主入口（Picocli `@Command`，注册 12 个子命令）
- [X] T011 创建 `ibios-cli/src/main/java/com/ibios/cli/IbiosSpringApp.java` 重命令 Spring 启动类（显式 `@EnableJpaRepositories`/`@EntityScan`）
- [X] T012 创建 `ibios-channel-cli/src/main/java/com/ibios/channel/cli/CliChannel.java`（chat 交互逻辑）
- [X] T013 创建 `ibios-channel-cli/src/main/java/com/ibios/channel/cli/ChatCommand.java`（Picocli `@Command`，调 CliChannel）
- [X] T014 [P] 创建轻命令骨架（`ibios-cli/`）：`InitCommand`、`StatusCommand`、`ProfileListCommand`、`ProfileCreateCommand`、`ProfileShowCommand`、`ProfileDeleteCommand`、`ProviderListCommand`、`ToolListCommand`、`SessionListCommand`
- [X] T015 [P] 创建重命令骨架（`ibios-cli/`）：`ServeCommand`（打印 TODO）、`GatewayCommand`（打印 TODO）

## Phase 5: 依赖配置 + 验证

- [X] T016 更新 `ibios-cli/pom.xml` 添加 ibios-storage + ibios-provider 依赖（重命令需要）
- [X] T017 更新 `ibios-channel-cli/pom.xml` 添加 ibios-storage + ibios-provider 依赖
- [X] T018 更新 `ibios-boot/pom.xml` 确保依赖链完整
- [X] T019 跑 `mvn clean verify` 全绿

## 依赖关系

- T001 → T002, T003（表先建好）
- T004 → T005 → T006（Session 扩展 → SessionManager 接口 → AgentService 更新）
- T002, T003, T005, T007 → T008, T009（实体和接口就绪后写测试）
- T010, T011 → T012, T013, T014, T015（CLI 骨架就绪后写命令）
- T016, T017 → T018 → T019（依赖配置完成后跑验证）

## 并行执行示例

- Phase 1: T001 → T002 + T003（并行）
- Phase 3: T008 + T009（并行）
- Phase 4: T014 + T015（并行）

## 实现策略

先做 Phase 1-3（数据层 + 测试），保证 Session 持久化和 SessionManager 幂等性通过测试。再做 Phase 4（CLI 命令），最后 Phase 5 验证。

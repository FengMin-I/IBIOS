# 任务清单：Notify 通知推送模块

## Phase 1: Core 接口 + Profile 字段变更

- [ ] T001 创建 `ibios-core/src/main/java/com/ibios/core/notify/NotifyChannelAdapter.java` 接口
- [ ] T002 [P] 创建 `ibios-core/src/main/java/com/ibios/core/notify/NotifyTarget.java` record
- [ ] T003 [P] 创建 `ibios-core/src/main/java/com/ibios/core/profile/NotifyChannelConfig.java` record
- [ ] T004 修改 `ibios-core/src/main/java/com/ibios/core/profile/Profile.java`：`notifyChannels` 从 `List<String>` 改为 `List<NotifyChannelConfig>`
- [ ] T005 修改 `ibios-core/src/main/java/com/ibios/core/profile/ProfileLoader.java`：适配 `notify_channels` 结构化解析

## Phase 2: WebhookNotifyAdapter 实现

- [ ] T006 更新 `ibios-tool/pom.xml`：添加 `spring-boot-starter`、`spring-web` 依赖
- [ ] T007 创建 `ibios-tool/src/main/java/com/ibios/tool/notify/WebhookNotifyAdapter.java`

## Phase 3: 验收 harness 测试（先于实现）

- [ ] T008 更新 `ibios-tool/pom.xml`：添加 `mockwebserver` 测试依赖
- [ ] T009 创建 `ibios-tool/src/test/java/com/ibios/tool/notify/WebhookNotifyAdapterTest.java`（MockWebServer：POST body 带 content、URL 来自 config、5xx 异常上抛）

## Phase 4: NotifyTools 骨架

- [ ] T010 创建 `ibios-tool/src/main/java/com/ibios/tool/builtin/NotifyTools.java`（骨架：ProfileContext 解析渠道、Sandbox 留调用位、adapter.send）

## Phase 5: 前序节测试适配 + 验证

- [ ] T011 更新 `ibios-core/src/test/java/com/ibios/core/profile/ProfileLoaderTest.java`：适配 `notifyChannels` 新类型
- [ ] T012 跑 `mvn clean verify` 全绿

## 依赖关系

- T001, T002, T003 → T004 → T005（接口先建好 → Profile 改字段 → Loader 适配）
- T001, T002 → T007（Adapter 实现依赖接口）
- T007 → T009（测试依赖实现）
- T001, T002 → T010（NotifyTools 依赖接口）
- T004 → T011（Profile 变更 → 测试适配）
- T009, T010, T011 → T012（全部完成后跑验证）

## 并行执行示例

- Phase 1: T001 + T002 + T003（并行）→ T004 → T005
- Phase 3: T008 + T009（串行，先加依赖再写测试）

## 实现策略

先做 Phase 1（Core 接口 + Profile 变更），保证接口定义就位。再做 Phase 2-3（Adapter 实现 + 测试），保证 webhook 发送可验。最后 Phase 4-5（NotifyTools 骨架 + 前序适配 + 验证）。

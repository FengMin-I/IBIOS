# 实现计划：Notify 通知推送模块

## 技术栈

JDK 21 + Spring Boot 3.x + Spring AI Alibaba（动手前先跑 mvn dependency:tree 确认锁定 BOM 里目标依赖存在）、SQLite + Spring Data JPA。凭证走环境变量占位，不落明文。SQLite 用手工建表脚本，不依赖 hibernate.ddl-auto=update。

## 模块落位

| 模块 | 交付物 |
|------|--------|
| ibios-core | `NotifyChannelAdapter` 接口、`NotifyTarget` record、Profile `notifyChannels` 字段改为结构化类型 `List<NotifyChannelConfig>` |
| ibios-tool | `WebhookNotifyAdapter`（核心阶段唯一实现）、`NotifyTools`（骨架，完整接线依赖 20/24 节） |

## 数据模型

本节不新增数据表。通知渠道配置存在 Profile 的 `notifyChannels` 字段中。

### Profile.notifyChannels 结构变更

```
原：List<String>
新：List<NotifyChannelConfig>

NotifyChannelConfig {
    type: String       // "webhook" 等
    name: String       // 渠道名称（Agent 按名引用）
    url: String        // webhook URL（支持 ${ENV_VAR} 占位）
}
```

## 架构设计

### 接口先行

```
NotifyChannelAdapter（ibios-core）
├── send(NotifyTarget target, String content)
│
└── WebhookNotifyAdapter（ibios-tool，核心阶段唯一实现）
    └── 用 RestClient POST JSON 到 target.config("url")
```

### NotifyTarget

```
NotifyTarget {
    channelType: String
    config: Map<String, String>
}
```

接口签名不出现任何具体渠道特有的词。

### NotifyTools（骨架）

```
NotifyTools
├── notify(content, channel)
│   ├── 从 ProfileContext 读当前 Profile
│   ├── 按 channel 名解析 NotifyChannelConfig → NotifyTarget
│   ├── sandbox.enforce(...)（留调用位，24 节接线）
│   └── adapter.send(target, content)
└── 依赖：ProfileContext（17 节）、Sandbox（24 节留位）、NotifyChannelAdapter
```

### Profile 配置示例

```yaml
notify_channels:
  - name: team
    type: webhook
    url: ${TEAM_WEBHOOK_URL}
```

## 测试策略

测试策略按课件"验收 harness"执行：

**第一批（本节立即可跑）——WebhookNotifyAdapterTest**（覆盖：POST body 带 content、URL 来自 NotifyTarget.config、5xx 异常上抛不吞），用 MockWebServer 起本地假 webhook。

**第二批（20/24 节接线后补跑）——NotifyToolsTest**（覆盖：notify_channels 未配置报错、channel 缺省取第一个、enforce 先于 send 被调用 InOrder 断言）。

单测默认跑、集成冒烟打 @Tag("integration") CI 跳过；实现完成的定义是 mvn clean verify 全绿。

## 语法禁区

避开 P3C/ASM 解析不了的 Java 18+ 语法形态（如增强 switch 的 default -> 写法），静态检查是构建门禁。

## 依赖变更

### ibios-core

- 新增 `NotifyChannelAdapter` 接口
- 新增 `NotifyTarget` record
- 新增 `NotifyChannelConfig` record（Profile 字段类型）
- 修改 `Profile` record：`notifyChannels` 从 `List<String>` 改为 `List<NotifyChannelConfig>`
- 修改 `ProfileLoader`：适配新字段类型

### ibios-tool

- 新增 `spring-boot-starter` 依赖（@Component）
- 新增 `spring-web` 依赖（RestClient）
- 新增 `notify/WebhookNotifyAdapter`
- 新增 `builtin/NotifyTools`（骨架）
- 测试依赖：`mockwebserver`（okhttp3）

### 前序节影响

- Profile record 签名变更 → ProfileLoaderTest 需更新
- ProfileLoader 解析逻辑变更 → 测试需更新

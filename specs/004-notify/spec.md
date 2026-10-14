# Feature Specification: Notify 通知推送模块

> Created by `/speckit-specify`。

## Metadata

- **Feature**: Notify 通知推送模块
- **Lesson**: 19
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. 背景与价值

CLI（18 节）让 Agent 能同步回话，但定时任务（25 节）等场景没有人在另一端等响应，Agent 必须主动把结果送到人能看到的地方。Notify 补的是出站推送这半边——入站有 Channel 负责"消息怎么进来"，Notify 负责"结果怎么主动送出去"。没有它，每个业务方都要在 Skill 里各写一份 webhook 推送逻辑，重复且不统一。

## 2. 用户场景

### 场景 1：定时任务推送到群

用户配好 Agent 每天查天气并推送穿搭建议到企业微信群。Agent 到点自动触发 ReAct 循环，最终调 `notify` 把结果送到配置好的 webhook 地址，群里收到消息。

### 场景 2：对话中主动推送

用户在对话中说"把测试消息推送一下"，Agent 调 `notify(content="测试消息")`，消息送到默认通知渠道。

### 场景 3：渠道未配置时报错

Agent 尝试调 `notify` 但当前 Profile 没有配 `notify_channels`，系统返回明确错误而不是静默失败。

## 3. 功能需求

- **FR1**：定义 `NotifyChannelAdapter` 接口，只有一个方法 `send(NotifyTarget target, String content)`，接口签名不出现任何具体渠道（企业微信/飞书/钉钉）特有的词。
- **FR2**：定义 `NotifyTarget` 记录类型，包含 `channelType`（String）和 `config`（Map<String, String>），具体配置由实现类解释。
- **FR3**：实现 `WebhookNotifyAdapter`（核心阶段唯一实现），用 HTTP POST 把 `content` 包成 JSON 发到 `NotifyTarget.config` 里的 `url`。
- **FR4**：`NotifyTools` 提供 `notify(content, channel)` 方法，从当前 Profile 的 `notify_channels` 配置中按名称解析目标渠道，调用 adapter 发送。`channel` 参数缺省时取第一个/默认渠道。
- **FR5**：`NotifyTools.notify` 发送前必须先调 `Sandbox.enforce`（Sandbox 未就位时留调用位，24 节接线），白名单校验不能被绕过。
- **FR6**：`notify_channels` 未配置时调 `notify` 必须明确报错，不能静默失败。
- **FR7**：webhook 返回 5xx 时异常向上抛，不静默吞掉。

## 4. 明确不做（边界）

- 不逐家接企业微信/飞书/钉钉的专用 API（签名算法、AccessToken 刷新）——核心阶段只用通用 webhook
- 不新增 Sandbox 逻辑——复用后续 24 节的统一 Sandbox 接口
- 不做 EmailNotifyAdapter（扩展阶段）
- 不做通知渠道的全局注册表 CRUD API（30 节 Web Service 再做）——核心阶段渠道配置在 Profile 的 `notify_channels` 字段
- `NotifyTools` 的完整 `@Tool` 注册和 `ToolResult` 接线在 20 节完成，本节先留接口位

## 5. 验收标准

### 自动化（Harness）

**第一批（本节立即可跑）——`WebhookNotifyAdapterTest`**：
- 用 MockWebServer 起本地假 webhook，发送后断言 POST 请求 body 带 `content`
- URL 来自 `NotifyTarget.config` 而不是硬编码
- webhook 返回 5xx 时异常向上抛、不静默吞掉

**第二批（20/24 节接线后补跑）——`NotifyToolsTest`**：
- `notify_channels` 未配置 → 明确报错
- `channel` 参数缺省 → 取第一个渠道
- `enforce` 先于 `send` 被调用（InOrder 断言）

### 人工

- `notify` 调用真的能把消息送到配置好的真实 webhook
- 接口中立性自查：换成企业微信官方 SDK 实现，`NotifyChannelAdapter.send(NotifyTarget, String)` 签名不需要改
- 渠道未配置报错、白名单先行、回归不破——由 harness 覆盖

## 6. 依赖与假设

### 前序节依赖

- **Lesson 16**：Profile（`notify_channels` 字段已存在于 Profile record）
- **Lesson 17**：ProfileContext（ThreadLocal，工具执行时读取当前 Agent 配置）
- **Lesson 18**：SessionManager/Session

### 后续节依赖（NotifyTools 完整接线）

- **Lesson 20**：`@Tool` 注解注册机制、`ToolResult` 返回类型
- **Lesson 24**：`Sandbox` 接口 + `Sandbox.enforce` 实现

### 外部依赖

- Spring Boot 自带的 `RestClient`（HTTP 发送）
- MockWebServer（测试用，okhttp3 mockwebserver）

---

## Constitution Check

- [x] Principle 1: JDK 21 + Spring Boot 3.x monolith
- [x] Principle 3: Self-implemented ReAct loop（Notify 不介入循环）
- [x] Principle 4: Spring AI protocol only（Notify 不用 Spring AI Agent 抽象）
- [x] Principle 5: Plugin tool three-tier access（NotifyTools 是内置 Tool）
- [x] Principle 6: Synchronous + virtual threads（RestClient 同步调用）
- [x] Principle 7: Sandbox interface-first（先调 Sandbox.enforce，细节 24 节）
- [x] Principle 10: No plaintext secrets（webhook URL 走 `${ENV_VAR}` 占位）

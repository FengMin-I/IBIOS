# Implementation Plan: Sandbox 白名单校验实现

> Created by `/speckit-plan`.

## Metadata

- **Feature**: Sandbox 白名单校验实现
- **Spec**: `specs/007-sandbox/spec.md`
- **Date**: 2026-10-15
- **Status**: approved

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists
in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data
JPA. Credentials via environment variable placeholders, no plaintext. SQLite
uses manual schema scripts, not `hibernate.ddl-auto=update`.

## 2. Module Placement

全部 → `ibios-tool`（和 FileTools / ShellTools / HttpTools 挨在一起）。

| Class / Package | Target Module |
|----------------|---------------|
| `WhitelistSandbox` | ibios-tool (`com.ibios.tool.sandbox`) |
| `FileSandboxProperties` | ibios-tool (`com.ibios.tool.sandbox`) |
| `ShellSandboxProperties` | ibios-tool (`com.ibios.tool.sandbox`) |
| `HttpSandboxProperties` | ibios-tool (`com.ibios.tool.sandbox`) |
| `WhitelistSandboxTest` | ibios-tool test |
| `SandboxIntegrationTest`（接线回归） | ibios-tool test |

**不新增 ibios-core 概念**——Sandbox 接口已在 Lesson 20 定死在 `com.ibios.core.tool`，本节只在 ibios-tool 填实现。

## 3. Design Decisions

### Decision 1: 课件包名适配

课件用 `io.ibios.tool.sandbox`，项目实际用 `com.ibios.tool.sandbox`。沿用项目约定。

### Decision 2: 语法禁区——增强 switch 降级

课件代码用 `case FILE_READ, FILE_WRITE ->`（增强 switch），P3C/ASM 解析不了。实现降级为传统 `switch-case-break`：

```java
switch (action.type()) {
  case FILE_READ:
  case FILE_WRITE:
    checkFilePath(action.target());
    break;
  case SHELL_COMMAND:
    checkShellCommand(action.target());
    break;
  case HTTP_REQUEST:
    checkHttpUrl(action.target());
    break;
  default:
    break;
}
```

### Decision 3: NoopSandbox 删除

`WhitelistSandbox` 替换 `NoopSandbox` 成为唯一 `Sandbox` Bean。删除 `NoopSandbox.java`，避免两个 `@Component` 实现同一接口导致 Spring 歧义。

### Decision 4: 配置为空 = 全部拒绝

`allowedPaths` / `allowedCommands` / `allowedDomains` 为空列表时，`anyMatch` 返回 false，所有请求被拒。这是安全默认值，不需要额外代码。

### Decision 5: NotifyTools 接线

NotifyTools 当前有 `TODO (Lesson 24)` 注释。本节在 webhook 发送前加 `sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, url))`，与 HttpTools 共享 `http.allowed_domains` 配置。

## 4. Test Strategy

Tests follow the lesson's acceptance harness: `WhitelistSandboxTest`（文件路径 / Shell 命令 / HTTP 域名三类各允许+拒绝+绕过场景）、`SandboxIntegrationTest`（四个 Tool 的接线回归：白名单外输入被拦 + IO 未发生）covering 路径穿越拦截、通配符点号边界、首 token 比对、空配置全拒。Unit tests run by default; integration smoke tests tagged `@Tag("integration")` skipped in CI. Implementation is done when `mvn clean verify` is all green.

## 5. Syntax Restrictions

Avoid Java 18+ syntax that P3C/ASM cannot parse (e.g., enhanced switch
`default ->` form). Static analysis is a build gate.

## 6. Constitution Check

- [x] Principle 7: Sandbox interface-first（接口不变，只填实现）
- [x] Principle 8: 审计 day-one（Sandbox 失败复用 tool_invocations）
- [x] Principle 10: 无明文 key
- [x] Module placement follows §10 dependency direction（全部 ibios-tool）
- [x] No reactive imports
- [x] No Spring AI auto-execution
- [x] Sandbox enforcement noted for IO paths

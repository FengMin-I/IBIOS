# Feature Specification: Sandbox 白名单校验实现

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: Sandbox 白名单校验实现
- **Lesson**: 24
- **Author**: IBIOS Team
- **Date**: 2026-10-15
- **Status**: draft

---

## 1. Background & Value

Agent 已经会想（ReAct）和会动手（Tool），但还没有"护栏"。Sandbox 模块补上这块：一个接口先行的安全墙，核心阶段挂一档白名单实现（文件路径 / Shell 命令 / HTTP 域名），接进已有的 FileTools / ShellTools / HttpTools / NotifyTools，校验失败走已有的 ToolExecutor 审计路径。接口设计对了，接入成本小到每个 Tool 只多一行代码。

## 2. User Scenarios

### Scenario 1: 文件路径白名单拦截

Agent 尝试读取 `/etc/passwd`，但白名单只允许 `.ibios/` 目录下的路径。Sandbox 拦截，抛 `SandboxViolationException`，ToolExecutor 记 `tool_invocations` 的 `success=false`，模型在下一轮看到失败原因。

### Scenario 2: Shell 命令白名单拦截

Agent 尝试执行 `rm -rf /`，但白名单只允许 `ls`、`cat` 等安全命令。Sandbox 取命令首 token 比对，拒绝执行，审计记录写入。

### Scenario 3: HTTP 域名白名单拦截

Agent 尝试向 `https://evil.com/steal` 发 POST 请求，但白名单只允许 `api.weather.com` 等域名。Sandbox 解析 host 后比对，拒绝请求。通配符 `*.example.com` 命中 `api.example.com` 但不命中 `evil-example.com`（点号边界回归）。

## 3. Functional Requirements

- **FR1**: `WhitelistSandbox` 实现 `Sandbox` 接口，按 `ActionType` 路由到三个私有校验方法（`checkFilePath` / `checkShellCommand` / `checkHttpUrl`），`FILE_READ` 和 `FILE_WRITE` 同路由到 `checkFilePath`
- **FR2**: 文件路径校验使用 `Path.normalize().toAbsolutePath()` 后 `startsWith` 比对白名单，防止 `../` 路径穿越
- **FR3**: Shell 命令校验取首 token（按空白分割）精确比对白名单
- **FR4**: HTTP URL 校验解析 host 后做域名匹配，支持精确匹配和通配符 `*.domain.com`（通配符匹配必须带点号边界，`*.example.com` 不命中 `evil-example.com`）
- **FR5**: 三块配置 `file.allowed_paths`（路径列表）、`shell.allowed_commands`（命令列表）、`http.allowed_domains`（域名列表，支持 `*.` 前缀通配符），通过 `@ConfigurationProperties` 绑定
- **FR6**: 任意校验失败抛 `SandboxViolationException`（RuntimeException），消息包含被拒绝的具体原因
- **FR7**: `FileTools` / `ShellTools` / `HttpTools` 在 `execute` 首行调 `sandbox.enforce(...)`（已在 Lesson 20 就位，本节验证）
- **FR8**: `NotifyTools` 在发送 webhook 前调 `sandbox.enforce(new SandboxAction(HTTP_REQUEST, url))`
- **FR9**: `WhitelistSandbox` 替换 `NoopSandbox` 成为唯一的 `Sandbox` Bean（删除 NoopSandbox）
- **FR10**: 配置白名单为空时，"什么都不允许"（空列表 = 全部拒绝），而非"不校验"

## 4. Out of Scope (Boundary)

- 容器隔离实现（namespace + cgroups + seccomp）——扩展阶段按信号驱动
- microVM 实现（Firecracker / Kata / gVisor）——扩展阶段
- 修改 `ToolExecutor` 的审计逻辑——Sandbox 失败走已有的工具失败路径
- `Sandbox` 接口签名变更——接口已在 Lesson 20 定死
- SMTP 端点白名单（`smtp.allowed_endpoints`）——核心阶段不做，EmailNotifyAdapter 在扩展阶段
- Profile 级 Tool Policy（哪个 Agent 能用哪些 Tool）——扩展阶段

## 5. Acceptance Criteria

### Automated (Harness)

- `WhitelistSandboxTest`：
  - 文件路径：白名单内放行 / 白名单外拒绝 / **相对路径穿越（`..` 爬出白名单目录）被拦**
  - Shell 命令：白名单内放行 / 白名单外拒绝 / 首 token 带前导空格的处理
  - HTTP 域名：精确匹配放行 / 白名单外拒绝 / **通配符 `*.example.com` 命中 `api.example.com` 但不命中 `evil-example.com`**
  - 配置为空 = 全部拒绝
- 接线回归：`FileTools` / `ShellTools` / `HttpTools` / `NotifyTools` 各一条——白名单外的输入被拦、**真正的 IO 没有发生**（mock 底层执行器，`verify(never())`）
- 改造后的四个 Tool 原有测试全绿（回归）

### Manual

- 集成验证：配只允许 `ls` 的白名单，真跑一次白名单外的命令，确认 `SandboxViolationException` 上抛、`tool_invocations` 有 `success=false` 记录
- 接口中立性自查：`Sandbox.enforce(SandboxAction)` 签名换成 microVM 实现能干净套入
- 配置边界写进文档：白名单为空 = "什么都不允许"

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- Lesson 17: `ToolExecutor` 已有失败审计路径（`success=false` + `error_message`）
- Lesson 20: `Sandbox` 接口、`SandboxAction`、`ActionType`、`SandboxViolationException` 已在 `ibios-core`；`FileTools` / `ShellTools` / `HttpTools` 已调 `sandbox.enforce(...)`；`NoopSandbox` 占位

### External Dependencies

- 无新增第三方依赖（`@ConfigurationProperties` 是 Spring Boot 自带）

---

## Constitution Check

- [x] Principle 7: Sandbox interface-first（接口已在 Lesson 20 定死，本节只填实现）
- [x] Principle 8: 审计 day-one（Sandbox 失败复用已有 `tool_invocations` 路径）
- [x] Principle 10: 无明文 key

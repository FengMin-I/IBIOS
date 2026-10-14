# 第 18 节规格说明：CLI 命令行入口——让 Agent 能在终端里对话

> 课件来源：`docs/class/第18节：CLI 功能概述、实现思路与代码讲解.md`
> 技术方案章节：§8.4（Channel 接入）、§8.6（三种运行模式）、§8.7（命令行工具）、§9.2（sessions 表）

---

## 背景与价值

有了 Provider（会调模型）和 ReAct（会思考的循环），IBIOS 的"大脑"已经能转了，但还缺一个能亲手操作它的入口。CLI 就是 IBIOS 的命令行入口——在终端里敲命令，跟 Agent 对话、把服务跑起来、查配置和状态。

IBIOS 打包出来是一个可执行 JAR，CLI 主入口是整个程序的 `main` 函数。所有操作通过子命令来做，核心阶段有 12 个。CLI 是消息进出的门，不是干活的人——干活的是引擎和下面三块能力。

---

## 用户场景

1. 开发者在终端里输入 `ibios chat`，进入交互式对话，一问一答，输入 `/quit` 退出。
2. 开发者输入 `ibios profile list`，秒回查看有哪些 Agent（Profile），不需要等 Spring 启动。
3. 开发者输入 `ibios chat --profile weather`，和指定的 Agent 对话，Session 自动按 channel+user+profile 三元组隔离。

---

## 功能需求

**FR1. CLI 主入口。** `OryxOsCli` 是整个程序的 `main` 入口，注册 12 个 `@Command` 子命令：`init`、`status`、`chat`、`serve`、`gateway`、`profile list/create/show/delete`、`provider list`、`tool list`、`session list`。

**FR2. 命令轻重分流。** 不需要调模型/跑引擎的命令（`init`、`profile list` 等轻命令）不启动 Spring 上下文，直接做文件操作，秒回。需要调模型/跑引擎的命令（`chat`、`serve`、`gateway` 等重命令）才启动 Spring 上下文。

**FR3. CliChannel（chat 命令）。** 读 stdin 写 stdout 实现交互式对话，维护一个当前 Session，每收到一行交给 `AgentService.process` 处理，直到用户输入 `/quit` 退出。

**FR4. SessionManager。** 对外三个方法：`getOrCreate(channel, user, profileName)`、`get(sessionId)`、`save(session)`。`session_id` 由 channel+user+profile 联合唯一生成，**拼接只发生在 SessionManager 内部这一处**，所有入口（CLI 传 `"cli"`、Web 传 `"web"`、定时传 `"scheduler"`）只提供三元组、不自己拼字符串。同一三元组的多次 `getOrCreate` 返回同一个 Session（幂等）。

**FR5. sessions 表持久化。** Session JPA 实体（字段：`session_id` 主键、`profile_name`、`channel`、`user_id`、`messages_json`、`status`、`created_at`、`last_active_at`、`archived_at`）、`SessionRepository`、手工建表脚本（SQLite 不依赖 `ddl-auto=update`）。对话历史整体序列化成 JSON 存 `messages_json` 一列，核心阶段不做按条拆表。

**FR6. 重命令 Spring 扫描范围。** 重命令启动 Spring 上下文时，启动类必须显式声明 `@EnableJpaRepositories(basePackages=...)` 和 `@EntityScan(basePackages=...)`，不能只靠 `scanBasePackages`——后两者是独立的扫描逻辑，不显式声明会导致 "Found 0 JPA repository interfaces"。

---

## 明确不做（边界）

- IM Channel 扩展（企业微信、飞书、钉钉、Slack）——扩展阶段做
- `serve` 命令的完整 Web Service 实现——第 26 节细讲
- `gateway` 命令的多通道挂载实现——后续节做
- 命令分流（轻命令不起 Spring）的自动化测试——成本大于收益，留人工确认
- 12 个命令的 `--help` 自动化测试——Picocli 自带，留人工确认

---

## 验收标准

可自动化部分由课件"验收 harness"测试套件承载（`mvn test` 全绿即通过）：

- **SessionManagerTest**：同一三元组两次 `getOrCreate` 返回同一个 Session（幂等）；channel/user/profile 任一不同则是不同 Session；id 生成只此一处
- **SessionRepositoryTest**：手工建表脚本建出的 `sessions` 表能存能读；`messages_json` 序列化回读后消息完整；模拟"重启"（新建 context 重查）历史还在

关键回归点：
- Session 幂等（同一三元组返回同一 Session）
- Session 隔离（不同三元组返回不同 Session）
- session_id 拼接只发生在 SessionManager 内部
- messages_json 序列化/反序列化完整性
- 重启后历史不丢

人工项见课件"五、做完怎么验"。

---

## 依赖与假设

- 依赖第 16 节交付物：Profile/ProfileLoader/ProfileRegistry（ibios-core）、ProviderService（ibios-provider）
- 依赖第 17 节交付物：ReActLoop/AgentService/ProfileContext（ibios-core）、Session（ibios-core）
- AgentService.SessionManager 接口已定义（ibios-core），本节提供实现
- 外部依赖：Picocli（BOM 已锁定）
- 模块落位：`OryxOsCli`+12 命令→ibios-cli；`CliChannel`→ibios-channel-cli；`SessionManager` 接口→ibios-core；`Session` 实体+Repository→ibios-storage

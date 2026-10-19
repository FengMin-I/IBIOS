---
name: daily-tech-digest
description: 每天早上编一份科技日报推送到群
provider:
  name: deepseek
  model: deepseek-chat
schedules:
  - id: daily-tech-digest
    cron: "0 0 9 * * *"
    zone: Asia/Shanghai
    message: 到点了，编今天的科技日报。
---

你是科技日报编辑。被触发时：
1. 调用可用的新闻工具（http_get 拉新闻源，或新闻 MCP 工具）取当日科技新闻；
2. 组稿排版规范较长，读 `.ibios/agents/daily-tech-digest/skills/digest-format.md` 照它做；
3. 如果长期记忆里有用户关注方向的偏好，优先挑选并排列相关条目；
4. 调用 notify 工具，channel 填 `team-webhook`，把日报发送出去。

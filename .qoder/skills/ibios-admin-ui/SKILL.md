# IBIOS Admin UI Skill

> 管理台前端设计规范，保证所有页面与 IBIOS 官网首页同源。

## 设计 Token

### 颜色

| Token | 值 | 用途 |
|-------|-----|------|
| --bg-primary | #000000 | 页面背景 |
| --bg-card | #111111 | 卡片背景 |
| --bg-hover | #1a1a1a | 悬浮/选中背景 |
| --border | #222222 | 分隔线、边框 |
| --accent | #f97316 | 主色（橙），仅用于强调 |
| --accent-hover | #ea6a00 | 主色 hover |
| --accent-dark | #c2550a | 主色 pressed |
| --text-primary | #f5f5f5 | 主文字 |
| --text-secondary | #a3a3a3 | 次要文字 |
| --text-muted | #666666 | 弱文字 |
| --success | #22c55e | 成功状态 |
| --error | #ef4444 | 错误状态 |
| --warning | #f97316 | 警告状态 |

### 字体

- **正文**: Inter, -apple-system, BlinkMacSystemFont, sans-serif
- **代码/ID/JSON**: JetBrains Mono, monospace

### 圆角

- 卡片: 6px
- 按钮/输入框: 4px
- 整体克制，不做大圆角

## 工程约定

- **技术栈**: Vue 3 + Vite
- **源码位置**: `ibios-web/src/main/frontend/`
- **构建产出**: `ibios-web/src/main/resources/static/admin/`
- **Vite base**: `/admin/`
- **SPA 回落**: Spring WebConfig 把 `/admin/**` 未命中路径回落到 `admin/index.html`
- **API 前缀**: 所有数据请求走 `/api/v1/*`
- **只读**: 核心阶段管理台只有查看功能，不暴露任何写操作入口

## 布局

- **左侧导航**: 固定宽度 240px，深色背景，顶部 logo + 标题，下方导航项
- **右侧内容区**: 自适应宽度，padding 32px
- **响应式**: 窄屏（<768px）导航宽度缩至 200px

## 三态规范

每个页面必须处理三种状态：

1. **加载中**: 居中显示"加载中..."
2. **错误**: 居中显示错误信息（红色）
3. **空数据**: 居中显示"暂无数据"或对应提示

## 组件规范

- **表格**: 深色背景，行分隔用 --border，表头用 --text-secondary
- **卡片**: --bg-card 背景，--border 边框，padding 20px
- **状态标签**: 小圆点或标签形式，成功绿、失败红、警告橙
- **代码/ID**: 用 JetBrains Mono 等宽字体

## 验收清单

- [ ] 深色主题，背景 #000000
- [ ] 主色橙色 #f97316 仅用于强调（激活项、链接、数值高亮）
- [ ] 正文 Inter，代码 JetBrains Mono
- [ ] 左侧导航 + 右侧内容区布局
- [ ] 五个页面（会话/Profile/Tool/记忆/状态）全部只读
- [ ] 所有数据来自 /api/v1 端点
- [ ] 加载中/错误/空数据三态都有明确占位
- [ ] 响应式，窄屏导航收窄
- [ ] vite build 产出到 static/admin/
- [ ] SPA 回落正常工作（刷新子路由不 404）

## 使用方式

生成管理台页面时，调用此 skill，产出天然与 IBIOS 官网首页同源、可复现。

```text
调用 ibios-admin-ui skill，生成 [页面名称] 页面
```

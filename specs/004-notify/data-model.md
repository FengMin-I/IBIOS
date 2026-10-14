# 数据模型：Notify 通知推送模块

## 本节新增

无新增数据表。通知渠道配置存在 Profile 的 `notifyChannels` 字段中。

## Profile 字段变更

### NotifyChannelConfig（新增 record）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 渠道名称，Agent 按名引用 |
| type | String | 渠道类型，核心阶段只支持 "webhook" |
| url | String | webhook URL，支持 `${ENV_VAR}` 占位 |

### NotifyTarget（新增 record，ibios-core）

| 字段 | 类型 | 说明 |
|------|------|------|
| channelType | String | 渠道类型 |
| config | Map<String, String> | 渠道特定配置（如 url） |

### Profile.notifyChannels 类型变更

```
原：List<String>
新：List<NotifyChannelConfig>
```

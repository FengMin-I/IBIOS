package com.ibios.tool.builtin;

import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyTarget;
import com.ibios.core.profile.NotifyChannelConfig;
import com.ibios.core.profile.Profile;
import com.ibios.core.react.ProfileContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通知工具：Agent 在 ReAct 循环中主动给用户发通知（邮件、Webhook、Slack 等）。
 *
 * <p>TODO (Lesson 20/24)：
 *
 * <ul>
 *   <li>Lesson 20：加 @Tool 注解，让 ToolRegistry 能扫描到
 *   <li>Lesson 24：执行前调 Sandbox.enforce 做沙箱检查
 * </ul>
 */
@Component
public class NotifyTools {

  private static final Logger log = LoggerFactory.getLogger(NotifyTools.class);

  private final NotifyChannelAdapter adapter;

  public NotifyTools(NotifyChannelAdapter adapter) {
    this.adapter = adapter;
  }

  /**
   * 发送通知到指定渠道。
   *
   * @param channelName Profile 中配置的 notify_channels.name
   * @param content 通知内容
   */
  // TODO (Lesson 20): @Tool(description = "Send notification to user via configured channels")
  public void sendNotification(String channelName, String content) {
    Profile profile = ProfileContext.current();
    if (profile == null) {
      log.warn("ProfileContext is null, cannot send notification");
      return;
    }

    NotifyChannelConfig channel =
        profile.notifyChannels().stream()
            .filter(c -> c.name().equals(channelName))
            .findFirst()
            .orElse(null);

    if (channel == null) {
      log.warn("Notify channel '{}' not found in profile", channelName);
      return;
    }

    // TODO (Lesson 24): Sandbox.enforce(channel.type(), channel.config())

    NotifyTarget target = new NotifyTarget(channel.type(), Map.of("url", channel.url()));
    adapter.send(target, content);
    log.info("Notification sent to channel '{}'", channelName);
  }
}

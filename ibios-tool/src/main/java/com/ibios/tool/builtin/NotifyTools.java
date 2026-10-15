package com.ibios.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyTarget;
import com.ibios.core.profile.NotifyChannelConfig;
import com.ibios.core.profile.Profile;
import com.ibios.core.react.ProfileContext;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotifyTools implements IbiosTool {

  private static final Logger log = LoggerFactory.getLogger(NotifyTools.class);

  private final NotifyChannelAdapter adapter;
  private final Sandbox sandbox;
  private final ObjectMapper objectMapper;

  public NotifyTools(NotifyChannelAdapter adapter, Sandbox sandbox) {
    this.adapter = adapter;
    this.sandbox = sandbox;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public String name() {
    return "notify";
  }

  @Override
  public String description() {
    return "Send notification to user via configured channels";
  }

  @Override
  public String inputSchema() {
    return """
        {"type":"object","properties":{"channel":{"type":"string","description":"Name of the notif\
        ication channel"},"content":{"type":"string","description":"Notification content"}},"requir\
        ed":["channel","content"]}""";
  }

  @Override
  public ToolExecutionResult execute(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String channelName = args.get("channel").asText();
      String content = args.get("content").asText();

      Profile profile = ProfileContext.current();
      if (profile == null) {
        return ToolExecutionResult.failure(
            "ProfileContext is null, cannot send notification", false);
      }

      NotifyChannelConfig channel =
          profile.notifyChannels().stream()
              .filter(c -> c.name().equals(channelName))
              .findFirst()
              .orElse(null);

      if (channel == null) {
        return ToolExecutionResult.failure(
            "Notify channel '" + channelName + "' not found in profile", false);
      }

      sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, channel.url()));

      NotifyTarget target = new NotifyTarget(channel.type(), Map.of("url", channel.url()));
      adapter.send(target, content);
      log.info("Notification sent to channel '{}'", channelName);
      return ToolExecutionResult.success("Notification sent to " + channelName);
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to send notification: " + e.getMessage(), false);
    }
  }
}

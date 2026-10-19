package com.ibios.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyChannelStore;
import com.ibios.core.notify.NotifyChannelView;
import com.ibios.core.notify.NotifyTarget;
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
  private static final int MAX_MEMORY_CHARS = 500;

  private final NotifyChannelAdapter adapter;
  private final Sandbox sandbox;
  private final ObjectMapper objectMapper;
  private final MemoryService memoryService;
  private final NotifyChannelStore channelStore;

  public NotifyTools(
      NotifyChannelAdapter adapter,
      Sandbox sandbox,
      MemoryService memoryService,
      NotifyChannelStore channelStore) {
    this.adapter = adapter;
    this.sandbox = sandbox;
    this.objectMapper = new ObjectMapper();
    this.memoryService = memoryService;
    this.channelStore = channelStore;
  }

  @Override
  public String name() {
    return "notify";
  }

  @Override
  public String description() {
    return "Send notification to user via configured channels and save to memory";
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

      NotifyChannelView channel = channelStore.getByName(channelName);

      if (channel == null) {
        return ToolExecutionResult.failure(
            "Notify channel '" + channelName + "' not found in registry", false);
      }

      if (channel.url() == null || channel.url().isBlank()) {
        return ToolExecutionResult.failure(
            "Notify channel '"
                + channelName
                + "' has empty URL. Please set the required environment variable.",
            false);
      }

      sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, channel.url()));

      NotifyTarget target = new NotifyTarget(channel.type(), Map.of("url", channel.url()));
      adapter.send(target, content);
      log.info("Notification sent to channel '{}'", channelName);

      if (memoryService != null) {
        String memoryContent = "[通知] 发送到 " + channelName + ": " + truncate(content);
        memoryService.remember(memoryContent, MemoryScope.ARCHIVAL);
        log.debug("Notification saved to memory");
      }

      return ToolExecutionResult.success("Notification sent to " + channelName);
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to send notification: " + e.getMessage(), false);
    }
  }

  private String truncate(String content) {
    if (content.length() <= MAX_MEMORY_CHARS) {
      return content;
    }
    return content.substring(0, MAX_MEMORY_CHARS) + "…";
  }
}

package com.ibios.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.storage.audit.LlmCallAudit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ProviderService implements ChatPort {

  private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

  private final Map<String, ChatModel> models;
  private final ToolSchemaAdapter toolSchemaAdapter;
  private final LlmCallAudit audit;
  private final ObjectMapper objectMapper;

  public ProviderService(
      ProviderChatModelFactory factory, ToolSchemaAdapter toolSchemaAdapter, LlmCallAudit audit) {
    this.models = factory.getModels();
    this.toolSchemaAdapter = toolSchemaAdapter;
    this.audit = audit;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public ChatResponse chat(String sessionId, Profile profile, Prompt prompt) {
    String providerName = profile.provider().name();
    ChatModel chatModel = models.get(providerName);
    if (chatModel == null) {
      throw new com.ibios.core.profile.ProviderNotFoundException(providerName);
    }

    List<org.springframework.ai.chat.messages.Message> springMessages = convertMessages(prompt);
    ToolCallingChatOptions options =
        (ToolCallingChatOptions)
            ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
    List<org.springframework.ai.tool.ToolCallback> callbacks =
        toolSchemaAdapter.adapt(prompt.availableTools());
    if (!callbacks.isEmpty()) {
      options.setToolCallbacks(callbacks);
    }

    org.springframework.ai.chat.prompt.Prompt springPrompt =
        new org.springframework.ai.chat.prompt.Prompt(springMessages, options);
    long start = System.currentTimeMillis();
    try {
      org.springframework.ai.chat.model.ChatResponse chatResponse = chatModel.call(springPrompt);
      long latency = System.currentTimeMillis() - start;
      ChatResponse response = toChatResponse(chatResponse);
      audit.record(
          sessionId,
          providerName,
          profile.provider().model(),
          toAuditUsage(response),
          true,
          null,
          latency);
      return response;
    } catch (Exception e) {
      long latency = System.currentTimeMillis() - start;
      audit.record(
          sessionId,
          providerName,
          profile.provider().model(),
          null,
          false,
          e.getMessage(),
          latency);
      throw e;
    }
  }

  private List<org.springframework.ai.chat.messages.Message> convertMessages(Prompt prompt) {
    List<org.springframework.ai.chat.messages.Message> result = new ArrayList<>();

    if (prompt.systemPrompt() != null && !prompt.systemPrompt().isEmpty()) {
      result.add(new SystemMessage(prompt.systemPrompt()));
    }

    for (Message msg : prompt.messages()) {
      switch (msg.role()) {
        case USER:
          result.add(new UserMessage(msg.content()));
          break;
        case ASSISTANT:
          result.add(new AssistantMessage(msg.content()));
          break;
        case SYSTEM:
          result.add(new SystemMessage(msg.content()));
          break;
        case TOOL:
          if (msg.toolName() != null) {
            String toolResultContent =
                "Tool " + msg.toolName() + " execution result:\n" + msg.content();
            result.add(new UserMessage(toolResultContent));
          }
          break;
      }
    }

    return result;
  }

  private ChatResponse toChatResponse(org.springframework.ai.chat.model.ChatResponse chatResponse) {
    Generation generation = chatResponse.getResult();
    if (generation == null) {
      return new ChatResponse("", Collections.emptyList());
    }
    AssistantMessage assistantMessage = generation.getOutput();
    String text = assistantMessage.getText();

    log.info("Assistant message text: {}", text);
    log.info("Has tool calls: {}", assistantMessage.hasToolCalls());

    List<ToolCallResponse> toolCalls = new ArrayList<>();

    // 优先使用 Spring AI 解析的工具调用
    if (assistantMessage.hasToolCalls()) {
      for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
        log.info("Tool call (Spring AI): {} - {}", tc.name(), tc.arguments());
        toolCalls.add(new ToolCallResponse(tc.name(), tc.arguments()));
      }
    } else {
      // 回退：从文本中解析 DSML 格式的工具调用
      toolCalls = parseToolCallsFromText(text);
      if (!toolCalls.isEmpty()) {
        log.info("Parsed {} tool calls from text", toolCalls.size());
      }
    }

    return new ChatResponse(text, toolCalls);
  }

  private static final Pattern TOOL_CALL_PATTERN =
      Pattern.compile(
          "<tool_call>\\s*<name>(.*?)</name>\\s*<arguments>(.*?)</arguments>\\s*</tool_call>",
          Pattern.DOTALL);

  private List<ToolCallResponse> parseToolCallsFromText(String text) {
    if (text == null || text.isEmpty()) {
      return Collections.emptyList();
    }
    List<ToolCallResponse> result = new ArrayList<>();

    // 尝试标准 tool_call 格式
    Matcher matcher = TOOL_CALL_PATTERN.matcher(text);
    while (matcher.find()) {
      result.add(new ToolCallResponse(matcher.group(1).trim(), matcher.group(2).trim()));
    }

    // 如果标准格式没有匹配到，尝试 DSML 格式（使用简单字符串解析）
    if (result.isEmpty() && text.contains("DSML") && text.contains("invoke")) {
      // 查找所有 invoke 块
      int invokeStart = text.indexOf("invoke name=\"");
      while (invokeStart != -1) {
        // 提取工具名称
        int nameStart = invokeStart + "invoke name=\"".length();
        int nameEnd = text.indexOf("\"", nameStart);
        if (nameEnd == -1) break;
        String toolName = text.substring(nameStart, nameEnd).trim();

        // 参数关闭标签是普通 </parameter>，invoke 关闭标签是 </｜｜DSML｜｜invoke>
        int invokeEnd = text.indexOf("invoke>", nameEnd);
        if (invokeEnd == -1) break;

        // 提取参数块
        String parametersBlock = text.substring(nameEnd + 1, invokeEnd);

        // 解析参数
        Map<String, Object> args = new LinkedHashMap<>();
        int paramStart = parametersBlock.indexOf("parameter name=\"");
        while (paramStart != -1) {
          int paramNameStart = paramStart + "parameter name=\"".length();
          int paramNameEnd = parametersBlock.indexOf("\"", paramNameStart);
          if (paramNameEnd == -1) break;
          String paramName = parametersBlock.substring(paramNameStart, paramNameEnd).trim();

          // 查找参数值的开始和结束
          int valueStart = parametersBlock.indexOf(">", paramNameEnd) + 1;
          int valueEnd = parametersBlock.indexOf("</", valueStart);
          if (valueEnd == -1) break;
          String paramValue = parametersBlock.substring(valueStart, valueEnd).trim();

          args.put(paramName, parseParamValue(paramValue));

          // 查找下一个参数
          paramStart = parametersBlock.indexOf("parameter name=\"", valueEnd);
        }

        // 转换为 JSON 字符串
        try {
          String argumentsJson = objectMapper.writeValueAsString(args);
          result.add(new ToolCallResponse(toolName, argumentsJson));
          log.info("DSML parsed tool: {} with {} parameters", toolName, args.size());
        } catch (Exception e) {
          log.warn("Failed to serialize DSML tool call arguments: {}", e.getMessage());
        }

        // 查找下一个 invoke
        invokeStart = text.indexOf("invoke name=\"", invokeEnd);
      }
    }

    return result;
  }

  private Object parseParamValue(String value) {
    String trimmed = value.trim();
    // DSML 参数没有类型信息，数组/对象字面量需保留结构，否则工具收到的是字符串
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      try {
        return objectMapper.readTree(trimmed);
      } catch (Exception e) {
        log.debug("DSML parameter is not valid JSON, keeping raw string: {}", e.getMessage());
      }
    }
    return value;
  }

  private LlmCallAudit.TokenUsage toAuditUsage(ChatResponse response) {
    return new LlmCallAudit.TokenUsage(0, 0, 0);
  }
}

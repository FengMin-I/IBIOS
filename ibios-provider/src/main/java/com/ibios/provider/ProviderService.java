package com.ibios.provider;

import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.storage.audit.LlmCallAudit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ProviderService implements ChatPort {

  private final Map<String, ChatModel> models;
  private final ToolSchemaAdapter toolSchemaAdapter;
  private final LlmCallAudit audit;

  public ProviderService(
      ProviderChatModelFactory factory, ToolSchemaAdapter toolSchemaAdapter, LlmCallAudit audit) {
    this.models = factory.getModels();
    this.toolSchemaAdapter = toolSchemaAdapter;
    this.audit = audit;
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

    List<ToolCallResponse> toolCalls = new ArrayList<>();
    if (assistantMessage.hasToolCalls()) {
      for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
        toolCalls.add(new ToolCallResponse(tc.name(), tc.arguments()));
      }
    }

    return new ChatResponse(text, toolCalls);
  }

  private LlmCallAudit.TokenUsage toAuditUsage(ChatResponse response) {
    return new LlmCallAudit.TokenUsage(0, 0, 0);
  }
}

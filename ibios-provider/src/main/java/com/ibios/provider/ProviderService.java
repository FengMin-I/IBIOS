package com.ibios.provider;

import com.ibios.core.profile.Profile;
import com.ibios.storage.audit.LlmCallAudit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {

  private final Map<String, ChatModel> models;
  private final ToolSchemaAdapter toolSchemaAdapter;
  private final LlmCallAudit audit;

  public ProviderService(
      ProviderChatModelFactory factory, ToolSchemaAdapter toolSchemaAdapter, LlmCallAudit audit) {
    this.models = factory.getModels();
    this.toolSchemaAdapter = toolSchemaAdapter;
    this.audit = audit;
  }

  public ProviderResponse chat(String sessionId, Profile profile, ProviderRequest request) {
    String providerName = profile.provider().name();
    ChatModel chatModel = models.get(providerName);
    if (chatModel == null) {
      throw new com.ibios.core.profile.ProviderNotFoundException(providerName);
    }

    UserMessage userMessage = new UserMessage(request.content());
    ToolCallingChatOptions options =
        (ToolCallingChatOptions)
            ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
    List<org.springframework.ai.tool.ToolCallback> callbacks =
        toolSchemaAdapter.adapt(request.availableTools());
    if (!callbacks.isEmpty()) {
      options.setToolCallbacks(callbacks);
    }

    Prompt prompt = new Prompt(userMessage, options);
    long start = System.currentTimeMillis();
    try {
      ChatResponse chatResponse = chatModel.call(prompt);
      long latency = System.currentTimeMillis() - start;
      ProviderResponse response = toProviderResponse(chatResponse);
      audit.record(
          sessionId,
          providerName,
          profile.provider().model(),
          toAuditUsage(response.usage()),
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

  private ProviderResponse toProviderResponse(ChatResponse chatResponse) {
    Generation generation = chatResponse.getResult();
    if (generation == null) {
      return new ProviderResponse(
          "", Collections.emptyList(), new ProviderResponse.TokenUsage(0, 0, 0));
    }
    AssistantMessage assistantMessage = generation.getOutput();
    String text = assistantMessage.getText();

    List<ProviderResponse.ToolCall> toolCalls = new ArrayList<>();
    if (assistantMessage.hasToolCalls()) {
      for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
        toolCalls.add(new ProviderResponse.ToolCall(tc.name(), tc.arguments()));
      }
    }

    ProviderResponse.TokenUsage usage = new ProviderResponse.TokenUsage(0, 0, 0);
    if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
      org.springframework.ai.chat.metadata.Usage aiUsage = chatResponse.getMetadata().getUsage();
      int prompt = aiUsage.getPromptTokens() != null ? aiUsage.getPromptTokens() : 0;
      int completion = aiUsage.getCompletionTokens() != null ? aiUsage.getCompletionTokens() : 0;
      int total = aiUsage.getTotalTokens() != null ? aiUsage.getTotalTokens() : 0;
      usage = new ProviderResponse.TokenUsage(prompt, completion, total);
    }
    return new ProviderResponse(text, toolCalls, usage);
  }

  private LlmCallAudit.TokenUsage toAuditUsage(ProviderResponse.TokenUsage usage) {
    return new LlmCallAudit.TokenUsage(
        usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
  }
}

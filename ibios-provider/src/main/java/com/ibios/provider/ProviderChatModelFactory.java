package com.ibios.provider;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class ProviderChatModelFactory {

  private final Map<String, ChatModel> models;

  public ProviderChatModelFactory(ProvidersProperties properties) {
    this.models = new HashMap<>();
    for (ProvidersProperties.ProviderEntry entry : properties.getProviders()) {
      if ("mock".equals(entry.getName())) {
        models.put(entry.getName(), new MockChatModel());
        continue;
      }
      if ("deepseek".equals(entry.getName())) {
        OpenAiApi api =
            OpenAiApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(entry.getApiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder().model(entry.getModel()).build();
        OpenAiChatModel chatModel =
            OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
        models.put(entry.getName(), chatModel);
        continue;
      }
      DashScopeApi api = DashScopeApi.builder().apiKey(entry.getApiKey()).build();
      DashScopeChatOptions options =
          DashScopeChatOptions.builder().withModel(entry.getModel()).build();
      DashScopeChatModel chatModel =
          DashScopeChatModel.builder().dashScopeApi(api).defaultOptions(options).build();
      models.put(entry.getName(), chatModel);
    }
  }

  public Map<String, ChatModel> getModels() {
    return models;
  }

  public ChatModel getModel(String providerName) {
    return models.get(providerName);
  }
}

package com.ibios.provider;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class ProviderChatModelFactory {

  private final Map<String, ChatModel> models;

  public ProviderChatModelFactory(ProvidersProperties properties) {
    this.models = new HashMap<>();
    for (ProvidersProperties.ProviderEntry entry : properties.getProviders()) {
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

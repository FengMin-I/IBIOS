package com.ibios.provider;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class ProviderChatModelFactory {

  private static final Logger log = LoggerFactory.getLogger(ProviderChatModelFactory.class);
  private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";

  private final Map<String, ChatModel> models;

  public ProviderChatModelFactory(ProvidersProperties properties) {
    this.models = new HashMap<>();
    for (ProvidersProperties.ProviderEntry entry : properties.getProviders()) {
      if ("mock".equals(entry.getName())) {
        models.put(entry.getName(), new MockChatModel());
        continue;
      }
      if ("dashscope".equals(entry.getName())) {
        models.put(entry.getName(), buildDashScope(entry));
        continue;
      }
      String baseUrl = resolveBaseUrl(entry);
      if (baseUrl == null) {
        log.warn(
            "Skip provider [{}]: base-url is required for OpenAI-compatible providers",
            entry.getName());
        continue;
      }
      models.put(entry.getName(), buildOpenAiCompatible(baseUrl, entry));
    }
  }

  private static OpenAiChatModel buildOpenAiCompatible(
      String baseUrl, ProvidersProperties.ProviderEntry entry) {
    OpenAiApi api = OpenAiApi.builder().baseUrl(baseUrl).apiKey(entry.getApiKey()).build();
    OpenAiChatOptions options = OpenAiChatOptions.builder().model(entry.getModel()).build();
    return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
  }

  private static DashScopeChatModel buildDashScope(ProvidersProperties.ProviderEntry entry) {
    DashScopeApi api = DashScopeApi.builder().apiKey(entry.getApiKey()).build();
    DashScopeChatOptions options =
        DashScopeChatOptions.builder().withModel(entry.getModel()).withEnableSearch(false).build();
    return DashScopeChatModel.builder().dashScopeApi(api).defaultOptions(options).build();
  }

  private static String resolveBaseUrl(ProvidersProperties.ProviderEntry entry) {
    String configured = entry.getBaseUrl();
    if (configured != null && !configured.isBlank()) {
      return normalizeBaseUrl(configured);
    }
    if ("deepseek".equals(entry.getName())) {
      return DEFAULT_DEEPSEEK_BASE_URL;
    }
    return null;
  }

  static String normalizeBaseUrl(String baseUrl) {
    String normalized = baseUrl.trim().replaceAll("/+$", "");
    if (normalized.endsWith("/v1")) {
      normalized = normalized.substring(0, normalized.length() - 3).replaceAll("/+$", "");
    }
    return normalized;
  }

  public Map<String, ChatModel> getModels() {
    return models;
  }

  public ChatModel getModel(String providerName) {
    return models.get(providerName);
  }
}

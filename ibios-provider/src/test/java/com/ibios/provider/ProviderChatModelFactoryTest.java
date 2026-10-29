package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;

@DisplayName("ProviderChatModelFactory 构建规则测试")
class ProviderChatModelFactoryTest {

  @Test
  @DisplayName("带 base-url 的自定义 Provider 构建为 OpenAiChatModel")
  void customBaseUrlBuildsOpenAiChatModel() {
    ProviderChatModelFactory factory =
        factoryOf(entry("custom", "https://api.example.com/v1", "test-key", "test-model"));

    assertInstanceOf(OpenAiChatModel.class, factory.getModel("custom"));
  }

  @Test
  @DisplayName("deepseek 未配置 base-url 时使用默认地址并构建为 OpenAiChatModel")
  void deepseekWithoutBaseUrlUsesDefaultEndpoint() {
    ProviderChatModelFactory factory =
        factoryOf(entry("deepseek", null, "test-key", "deepseek-chat"));

    assertInstanceOf(OpenAiChatModel.class, factory.getModel("deepseek"));
  }

  @Test
  @DisplayName("dashscope 构建为 DashScopeChatModel")
  void dashscopeBuildsDashScopeChatModel() {
    ProviderChatModelFactory factory = factoryOf(entry("dashscope", null, "test-key", "qwen-plus"));

    assertInstanceOf(DashScopeChatModel.class, factory.getModel("dashscope"));
  }

  @Test
  @DisplayName("未配置 base-url 的自定义 Provider 被跳过")
  void customProviderWithoutBaseUrlIsSkipped() {
    ProviderChatModelFactory factory = factoryOf(entry("custom", "  ", "test-key", "test-model"));

    assertNull(factory.getModel("custom"));
  }

  @Test
  @DisplayName("base-url 末尾的 /v1 与斜杠会被归一化")
  void baseUrlNormalizationStripsVersionSuffix() {
    assertEquals(
        "https://api.example.com",
        ProviderChatModelFactory.normalizeBaseUrl("https://api.example.com/v1/"));
    assertEquals(
        "https://api.example.com/api",
        ProviderChatModelFactory.normalizeBaseUrl("https://api.example.com/api/v1"));
  }

  private static ProviderChatModelFactory factoryOf(ProvidersProperties.ProviderEntry entry) {
    ProvidersProperties properties = new ProvidersProperties();
    properties.setProviders(List.of(entry));
    return new ProviderChatModelFactory(properties);
  }

  private static ProvidersProperties.ProviderEntry entry(
      String name, String baseUrl, String apiKey, String model) {
    ProvidersProperties.ProviderEntry entry = new ProvidersProperties.ProviderEntry();
    entry.setName(name);
    entry.setBaseUrl(baseUrl);
    entry.setApiKey(apiKey);
    entry.setModel(model);
    return entry;
  }
}

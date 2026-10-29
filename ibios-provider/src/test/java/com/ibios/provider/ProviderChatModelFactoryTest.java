package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

@DisplayName("ProviderChatModelFactory 构建规则测试")
class ProviderChatModelFactoryTest {

  private final ProviderStore store = mock(ProviderStore.class);
  private final ProviderChatModelFactory factory = new ProviderChatModelFactory(store);

  @Test
  @DisplayName("type=openai 且有 base-url → OpenAiChatModel")
  void openAiTypeBuildsOpenAiChatModel() {
    given(view("custom", "openai", "https://api.example.com/v1", "test-key", "test-model"));

    assertInstanceOf(OpenAiChatModel.class, factory.getModel("custom"));
  }

  @Test
  @DisplayName("deepseek 未配 base-url 时用默认地址")
  void deepseekWithoutBaseUrlUsesDefaultEndpoint() {
    given(view("deepseek", "openai", null, "test-key", "deepseek-chat"));

    assertInstanceOf(OpenAiChatModel.class, factory.getModel("deepseek"));
  }

  @Test
  @DisplayName("type=dashscope → DashScopeChatModel")
  void dashscopeTypeBuildsDashScopeChatModel() {
    given(view("dashscope", "dashscope", null, "test-key", "qwen-plus"));

    assertInstanceOf(DashScopeChatModel.class, factory.getModel("dashscope"));
  }

  @Test
  @DisplayName("type=mock → MockChatModel")
  void mockTypeBuildsMockChatModel() {
    given(view("mock", "mock", null, null, null));

    assertInstanceOf(MockChatModel.class, factory.getModel("mock"));
  }

  @Test
  @DisplayName("openai 类型缺 base-url 且不是 deepseek → 返回 null")
  void openAiTypeWithoutBaseUrlReturnsNull() {
    given(view("custom", "openai", "  ", "test-key", "test-model"));

    assertNull(factory.getModel("custom"));
  }

  @Test
  @DisplayName("库里不存在的 provider → null")
  void unknownProviderReturnsNull() {
    assertNull(factory.getModel("missing"));
  }

  @Test
  @DisplayName("同一 provider 只建一次模型，invalidate 后重建")
  void cachesModelUntilInvalidated() {
    given(view("deepseek", "openai", null, "test-key", "deepseek-chat"));

    ChatModel first = factory.getModel("deepseek");
    ChatModel second = factory.getModel("deepseek");
    assertSame(first, second);
    verify(store, times(1)).getByName("deepseek");

    factory.invalidate("deepseek");
    assertInstanceOf(OpenAiChatModel.class, factory.getModel("deepseek"));
    verify(store, times(2)).getByName("deepseek");
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

  private void given(ProviderView view) {
    when(store.getByName(view.name())).thenReturn(view);
  }

  private static ProviderView view(
      String name, String type, String baseUrl, String apiKey, String model) {
    return new ProviderView(1L, name, type, apiKey, baseUrl, model);
  }
}

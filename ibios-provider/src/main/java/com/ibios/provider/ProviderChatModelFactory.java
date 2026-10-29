package com.ibios.provider;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/** 按 provider 名懒建并缓存 ChatModel：配置存在 DB 里，改完 {@link #invalidate(String)} 即可即时生效。 */
@Component
public class ProviderChatModelFactory {

  private static final Logger log = LoggerFactory.getLogger(ProviderChatModelFactory.class);
  private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";

  private final ProviderStore store;
  private final Map<String, ChatModel> cache = new ConcurrentHashMap<>();

  public ProviderChatModelFactory(ProviderStore store) {
    this.store = store;
  }

  public ChatModel getModel(String providerName) {
    ChatModel cached = cache.get(providerName);
    if (cached != null) {
      return cached;
    }
    ChatModel built = build(providerName);
    if (built == null) {
      return null;
    }
    ChatModel existing = cache.putIfAbsent(providerName, built);
    return existing != null ? existing : built;
  }

  public void invalidate(String providerName) {
    cache.remove(providerName);
  }

  private ChatModel build(String providerName) {
    ProviderView view = store.getByName(providerName);
    if (view == null) {
      return null;
    }
    String type = view.type() == null ? "openai" : view.type().trim();
    if ("mock".equals(type)) {
      return new MockChatModel();
    }
    if ("dashscope".equals(type)) {
      return buildDashScope(view);
    }
    String baseUrl = resolveBaseUrl(view);
    if (baseUrl == null) {
      log.warn(
          "Cannot build provider [{}]: base-url is required for OpenAI-compatible providers",
          providerName);
      return null;
    }
    return buildOpenAiCompatible(baseUrl, view);
  }

  private static OpenAiChatModel buildOpenAiCompatible(String baseUrl, ProviderView view) {
    OpenAiApi api = OpenAiApi.builder().baseUrl(baseUrl).apiKey(view.apiKey()).build();
    OpenAiChatOptions options = OpenAiChatOptions.builder().model(view.model()).build();
    return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
  }

  private static DashScopeChatModel buildDashScope(ProviderView view) {
    DashScopeApi api = DashScopeApi.builder().apiKey(view.apiKey()).build();
    DashScopeChatOptions options =
        DashScopeChatOptions.builder().withModel(view.model()).withEnableSearch(false).build();
    return DashScopeChatModel.builder().dashScopeApi(api).defaultOptions(options).build();
  }

  private static String resolveBaseUrl(ProviderView view) {
    String configured = view.baseUrl();
    if (configured != null && !configured.isBlank()) {
      return normalizeBaseUrl(configured);
    }
    if ("deepseek".equals(view.name())) {
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
}

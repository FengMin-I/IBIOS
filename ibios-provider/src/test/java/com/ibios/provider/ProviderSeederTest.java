package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProviderSeederTest {

  private final ProviderStore store = mock(ProviderStore.class);

  @Test
  @DisplayName("表为空 → 按 config 播种，并推断 type / deepseek 默认 base-url")
  void seedsWhenTableEmpty() {
    when(store.isEmpty()).thenReturn(true);
    when(store.create(any(), any(), any(), any(), any()))
        .thenAnswer(
            inv ->
                new ProviderView(
                    1L,
                    inv.getArgument(0),
                    inv.getArgument(1),
                    inv.getArgument(2),
                    inv.getArgument(3),
                    inv.getArgument(4)));

    ProvidersProperties properties = new ProvidersProperties();
    properties.setProviders(
        List.of(
            entry("deepseek", null, "sk-deepseek", "deepseek-chat"),
            entry("dashscope", null, "sk-dash", "qwen-plus")));

    new ProviderSeeder(store, properties).seed();

    ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> baseUrl = ArgumentCaptor.forClass(String.class);
    verify(store, times(2)).create(any(), type.capture(), any(), baseUrl.capture(), any());
    assertEquals(List.of("openai", "dashscope"), type.getAllValues());
    assertEquals("https://api.deepseek.com", baseUrl.getAllValues().get(0));
    assertEquals(null, baseUrl.getAllValues().get(1));
  }

  @Test
  @DisplayName("表非空 → 不播种，避免重启覆盖管理页改动")
  void skipsWhenTableNotEmpty() {
    when(store.isEmpty()).thenReturn(false);

    ProvidersProperties properties = new ProvidersProperties();
    properties.setProviders(List.of(entry("deepseek", null, "sk-deepseek", "deepseek-chat")));

    new ProviderSeeder(store, properties).seed();

    verify(store, never()).create(any(), any(), any(), any(), any());
    assertTrue(true);
  }

  @Test
  @DisplayName("config 里显式 type 优先")
  void explicitTypeWins() {
    when(store.isEmpty()).thenReturn(true);
    when(store.create(any(), any(), any(), any(), any()))
        .thenReturn(new ProviderView(1L, "mock", "mock", null, null, null));

    ProvidersProperties.ProviderEntry entry = entry("mock", null, null, null);
    entry.setType("mock");
    ProvidersProperties properties = new ProvidersProperties();
    properties.setProviders(List.of(entry));

    new ProviderSeeder(store, properties).seed();

    verify(store).create("mock", "mock", null, null, null);
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

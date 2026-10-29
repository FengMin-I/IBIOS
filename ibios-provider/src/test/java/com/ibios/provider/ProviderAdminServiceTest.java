package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProviderAdminService 测试")
class ProviderAdminServiceTest {

  private ProviderStore store;
  private ProviderChatModelFactory modelFactory;
  private ProfileRegistry profileRegistry;
  private ProviderAdminService service;

  @BeforeEach
  void setUp() {
    store = mock(ProviderStore.class);
    modelFactory = mock(ProviderChatModelFactory.class);
    profileRegistry = new ProfileRegistry();
    service = new ProviderAdminService(store, modelFactory, profileRegistry);
  }

  @Test
  @DisplayName("列表脱敏、详情明文")
  void listMasksKeys() {
    ProviderView view = new ProviderView(1L, "deepseek", "openai", "sk-abcdefghf847", null, "m");
    when(store.listAll()).thenReturn(List.of(view));
    when(store.getById(1L)).thenReturn(view);

    assertEquals("sk-****f847", service.list().get(0).apiKey());
    assertEquals("sk-abcdefghf847", service.get(1L).apiKey());
  }

  @Test
  @DisplayName("空 key 与超短 key 的脱敏结果")
  void maskHandlesEdgeCases() {
    assertEquals("", ProviderAdminService.maskKey(null));
    assertEquals("", ProviderAdminService.maskKey("  "));
    assertEquals("sk-****", ProviderAdminService.maskKey("sk-1"));
  }

  @Test
  @DisplayName("更新时空 key（或回传的掩码）保留原 key，并失效缓存")
  void updateKeepsExistingKeyWhenBlank() {
    ProviderView existing = new ProviderView(1L, "deepseek", "openai", "sk-original", null, "m");
    when(store.getById(1L)).thenReturn(existing);
    when(store.update(1L, "openai", "sk-original", null, "m2")).thenReturn(existing);

    service.update(1L, "openai", "sk-****inal", null, "m2");

    verify(store).update(1L, "openai", "sk-original", null, "m2");
    verify(modelFactory).invalidate("deepseek");
  }

  @Test
  @DisplayName("更新传了新 key 则用新 key")
  void updateUsesNewKey() {
    ProviderView existing = new ProviderView(1L, "deepseek", "openai", "sk-old", null, "m");
    when(store.getById(1L)).thenReturn(existing);
    when(store.update(1L, "openai", "sk-new", null, "m")).thenReturn(existing);

    service.update(1L, "openai", "sk-new", null, "m");

    verify(store).update(1L, "openai", "sk-new", null, "m");
  }

  @Test
  @DisplayName("被 Agent 引用的 Provider 不能删除")
  void deleteBlockedWhenReferenced() {
    when(store.getById(1L))
        .thenReturn(new ProviderView(1L, "deepseek", "openai", "sk-x", null, "m"));
    profileRegistry.register(profileOf("daily-news", "deepseek"));

    ProviderInUseException ex =
        assertThrows(ProviderInUseException.class, () -> service.delete(1L));
    assertTrue(ex.getMessage().contains("daily-news"));
    verify(store, never()).delete(any());
  }

  @Test
  @DisplayName("无人引用可删除，并失效缓存")
  void deleteRemovesUnusedProvider() {
    when(store.getById(1L))
        .thenReturn(new ProviderView(1L, "deepseek", "openai", "sk-x", null, "m"));

    service.delete(1L);

    verify(store).delete(1L);
    verify(modelFactory).invalidate("deepseek");
  }

  @Test
  @DisplayName("删除不存在的 Provider 是空操作")
  void deleteMissingIsNoop() {
    when(store.getById(9L)).thenReturn(null);

    service.delete(9L);

    verify(store, never()).delete(any());
  }

  @Test
  @DisplayName("get 找不到返回 null")
  void getMissingReturnsNull() {
    assertNull(service.get(404L));
  }

  private static Profile profileOf(String name, String providerName) {
    return new Profile(
        name,
        "desc",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig(providerName, "m", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20,
        null);
  }
}

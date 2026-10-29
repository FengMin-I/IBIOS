package com.ibios.provider;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import java.util.List;
import org.springframework.stereotype.Service;

/** Provider 管理面：列表脱敏、详情明文、改完即失效模型缓存。 */
@Service
public class ProviderAdminService {

  private final ProviderStore store;
  private final ProviderChatModelFactory modelFactory;
  private final ProfileRegistry profileRegistry;

  public ProviderAdminService(
      ProviderStore store, ProviderChatModelFactory modelFactory, ProfileRegistry profileRegistry) {
    this.store = store;
    this.modelFactory = modelFactory;
    this.profileRegistry = profileRegistry;
  }

  public List<ProviderView> list() {
    return store.listAll().stream().map(ProviderAdminService::mask).toList();
  }

  public ProviderView get(Long id) {
    return store.getById(id);
  }

  public ProviderView create(
      String name, String type, String apiKey, String baseUrl, String model) {
    return store.create(name, type, apiKey, baseUrl, model);
  }

  public ProviderView update(Long id, String type, String apiKey, String baseUrl, String model) {
    ProviderView existing = store.getById(id);
    if (existing == null) {
      return null;
    }
    String effectiveKey =
        apiKey == null || apiKey.isBlank() || isMasked(apiKey) ? existing.apiKey() : apiKey;
    ProviderView updated = store.update(id, type, effectiveKey, baseUrl, model);
    if (updated != null) {
      modelFactory.invalidate(updated.name());
    }
    return updated;
  }

  public void delete(Long id) {
    ProviderView existing = store.getById(id);
    if (existing == null) {
      return;
    }
    String referencedBy = findReferencingAgent(existing.name());
    if (referencedBy != null) {
      throw new ProviderInUseException(existing.name(), referencedBy);
    }
    store.delete(id);
    modelFactory.invalidate(existing.name());
  }

  private String findReferencingAgent(String providerName) {
    for (Profile profile : profileRegistry.getAll()) {
      if (profile.provider() != null && providerName.equals(profile.provider().name())) {
        return profile.name();
      }
    }
    return null;
  }

  static ProviderView mask(ProviderView view) {
    return new ProviderView(
        view.id(), view.name(), view.type(), maskKey(view.apiKey()), view.baseUrl(), view.model());
  }

  static String maskKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      return "";
    }
    if (apiKey.length() <= "sk-****".length()) {
      return "sk-****";
    }
    String prefix = apiKey.length() >= 3 ? apiKey.substring(0, 3) : "";
    String suffix = apiKey.substring(apiKey.length() - 4);
    return prefix + "****" + suffix;
  }

  static boolean isMasked(String value) {
    return value.contains("****");
  }
}

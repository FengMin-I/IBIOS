package com.ibios.provider;

import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 首次启动时把 application.yaml 里的 ibios.providers 播种进 providers 表。
 *
 * <p>只在表为空时播种一次：之后以 DB 为准，改 yaml 不会再覆盖（否则管理页改完的配置会被重启抹掉）。
 */
@Component
public class ProviderSeeder {

  private static final Logger log = LoggerFactory.getLogger(ProviderSeeder.class);
  private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";

  private final ProviderStore store;
  private final ProvidersProperties properties;

  public ProviderSeeder(ProviderStore store, ProvidersProperties properties) {
    this.store = store;
    this.properties = properties;
  }

  public void seed() {
    if (!store.isEmpty()) {
      log.info("providers table is not empty, skip seeding from config");
      return;
    }
    int seeded = 0;
    for (ProvidersProperties.ProviderEntry entry : properties.getProviders()) {
      if (entry.getName() == null || entry.getName().isBlank()) {
        continue;
      }
      ProviderView created =
          store.create(
              entry.getName(),
              resolveType(entry),
              entry.getApiKey(),
              resolveBaseUrl(entry),
              entry.getModel());
      seeded++;
      log.info(
          "Seeded provider [{}] type={} model={}", created.name(), created.type(), created.model());
    }
    log.info("Seeded {} provider(s) from config", seeded);
  }

  static String resolveType(ProvidersProperties.ProviderEntry entry) {
    if (entry.getType() != null && !entry.getType().isBlank()) {
      return entry.getType().trim();
    }
    if ("mock".equals(entry.getName())) {
      return "mock";
    }
    if ("dashscope".equals(entry.getName())) {
      return "dashscope";
    }
    return "openai";
  }

  static String resolveBaseUrl(ProvidersProperties.ProviderEntry entry) {
    if (entry.getBaseUrl() != null && !entry.getBaseUrl().isBlank()) {
      return entry.getBaseUrl().trim();
    }
    if ("deepseek".equals(entry.getName())) {
      return DEFAULT_DEEPSEEK_BASE_URL;
    }
    return null;
  }
}

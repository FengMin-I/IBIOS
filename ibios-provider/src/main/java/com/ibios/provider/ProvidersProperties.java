package com.ibios.provider;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ibios")
public class ProvidersProperties {

  private List<ProviderEntry> providers = new ArrayList<>();

  public List<ProviderEntry> getProviders() {
    return providers;
  }

  public void setProviders(List<ProviderEntry> providers) {
    this.providers = providers;
  }

  public static class ProviderEntry {

    private String name;
    private String apiKey;
    private String model;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }
}

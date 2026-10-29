package com.ibios.storage.provider;

import com.ibios.core.provider.ProviderStore;
import com.ibios.core.provider.ProviderView;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaProviderStore implements ProviderStore {

  private final ProviderRepository repository;

  public JpaProviderStore(ProviderRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ProviderView> listAll() {
    return repository.findAll().stream().map(this::toView).toList();
  }

  @Override
  public ProviderView getById(Long id) {
    return repository.findById(id).map(this::toView).orElse(null);
  }

  @Override
  public ProviderView getByName(String name) {
    return repository.findByName(name).map(this::toView).orElse(null);
  }

  @Override
  @Transactional
  public ProviderView create(
      String name, String type, String apiKey, String baseUrl, String model) {
    String providerName = requireName(name);
    String providerType = requireType(type);
    if (repository.existsByName(providerName)) {
      throw new IllegalArgumentException("Provider already exists: " + providerName);
    }
    ProviderEntity entity = new ProviderEntity();
    entity.setName(providerName);
    entity.setType(providerType);
    entity.setApiKey(apiKey);
    entity.setBaseUrl(baseUrl);
    entity.setModel(model);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return toView(repository.save(entity));
  }

  @Override
  @Transactional
  public ProviderView update(Long id, String type, String apiKey, String baseUrl, String model) {
    ProviderEntity entity = repository.findById(id).orElse(null);
    if (entity == null) {
      return null;
    }
    entity.setType(requireType(type));
    entity.setApiKey(apiKey);
    entity.setBaseUrl(baseUrl);
    entity.setModel(model);
    entity.setUpdatedAt(Instant.now());
    return toView(repository.save(entity));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    repository.findById(id).ifPresent(repository::delete);
  }

  @Override
  public boolean isEmpty() {
    return repository.count() == 0;
  }

  private String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Provider name is required");
    }
    return name.trim();
  }

  private String requireType(String type) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Provider type is required");
    }
    return type.trim();
  }

  private ProviderView toView(ProviderEntity entity) {
    return new ProviderView(
        entity.getId(),
        entity.getName(),
        entity.getType(),
        entity.getApiKey(),
        entity.getBaseUrl(),
        entity.getModel());
  }
}

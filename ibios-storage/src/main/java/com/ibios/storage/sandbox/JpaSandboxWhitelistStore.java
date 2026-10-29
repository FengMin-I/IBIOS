package com.ibios.storage.sandbox;

import com.ibios.core.sandbox.SandboxWhitelistStore;
import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaSandboxWhitelistStore implements SandboxWhitelistStore {

  private final SandboxWhitelistRepository repository;

  public JpaSandboxWhitelistStore(SandboxWhitelistRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<WhitelistEntryView> listAll() {
    return repository.findAllByOrderByTypeAscIdAsc().stream().map(this::toView).toList();
  }

  @Override
  public WhitelistEntryView findById(Long id) {
    return repository.findById(id).map(this::toView).orElse(null);
  }

  @Override
  public boolean exists(WhitelistType type, String value) {
    return repository.existsByTypeAndValue(type, value);
  }

  @Override
  @Transactional
  public WhitelistEntryView create(WhitelistType type, String value) {
    if (type == null) {
      throw new IllegalArgumentException("Whitelist type is required");
    }
    String entryValue = requireValue(value);
    if (repository.existsByTypeAndValue(type, entryValue)) {
      throw new IllegalArgumentException("Whitelist entry already exists: " + entryValue);
    }
    SandboxWhitelistEntity entity = new SandboxWhitelistEntity();
    entity.setType(type);
    entity.setValue(entryValue);
    entity.setCreatedAt(Instant.now());
    return toView(repository.save(entity));
  }

  @Override
  @Transactional
  public boolean delete(Long id) {
    return repository
        .findById(id)
        .map(
            entity -> {
              repository.delete(entity);
              return true;
            })
        .orElse(false);
  }

  private String requireValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Whitelist value is required");
    }
    return value.trim();
  }

  private WhitelistEntryView toView(SandboxWhitelistEntity entity) {
    return new WhitelistEntryView(entity.getId(), entity.getType(), entity.getValue());
  }
}

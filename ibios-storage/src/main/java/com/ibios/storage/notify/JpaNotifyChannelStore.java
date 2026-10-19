package com.ibios.storage.notify;

import com.ibios.core.notify.NotifyChannelStore;
import com.ibios.core.notify.NotifyChannelView;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaNotifyChannelStore implements NotifyChannelStore {

  private final NotifyChannelRepository repository;

  public JpaNotifyChannelStore(NotifyChannelRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<NotifyChannelView> listAll() {
    return repository.findAll().stream().map(this::toView).toList();
  }

  @Override
  public NotifyChannelView getById(Long id) {
    return repository.findById(id).map(this::toView).orElse(null);
  }

  @Override
  public NotifyChannelView getByName(String name) {
    return repository.findByName(name).map(this::toView).orElse(null);
  }

  @Override
  @Transactional
  public NotifyChannelView create(String name, String type, String url, String description) {
    String channelName = requireName(name);
    String channelType = requireType(type);
    if (repository.existsByName(channelName)) {
      throw new IllegalArgumentException("Notify channel already exists: " + channelName);
    }
    NotifyChannelEntity entity = new NotifyChannelEntity();
    entity.setName(channelName);
    entity.setType(channelType);
    entity.setUrl(url);
    entity.setDescription(description);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return toView(repository.save(entity));
  }

  @Override
  @Transactional
  public NotifyChannelView update(
      Long id, String name, String type, String url, String description) {
    NotifyChannelEntity entity = repository.findById(id).orElse(null);
    if (entity == null) {
      return null;
    }
    String channelName = requireName(name);
    String channelType = requireType(type);
    if (repository.existsByNameAndIdNot(channelName, id)) {
      throw new IllegalArgumentException("Notify channel already exists: " + channelName);
    }
    entity.setName(channelName);
    entity.setType(channelType);
    entity.setUrl(url);
    entity.setDescription(description);
    entity.setUpdatedAt(Instant.now());
    return toView(repository.save(entity));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    repository.findById(id).ifPresent(repository::delete);
  }

  private String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Notify channel name is required");
    }
    return name.trim();
  }

  private String requireType(String type) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Notify channel type is required");
    }
    return type.trim();
  }

  private NotifyChannelView toView(NotifyChannelEntity entity) {
    return new NotifyChannelView(
        entity.getId(),
        entity.getName(),
        entity.getType(),
        entity.getUrl(),
        entity.getDescription());
  }
}

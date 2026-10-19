package com.ibios.storage.notify;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyChannelRepository extends JpaRepository<NotifyChannelEntity, Long> {

  Optional<NotifyChannelEntity> findByName(String name);

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);
}

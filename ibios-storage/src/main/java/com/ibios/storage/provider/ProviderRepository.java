package com.ibios.storage.provider;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<ProviderEntity, Long> {

  Optional<ProviderEntity> findByName(String name);

  boolean existsByName(String name);
}

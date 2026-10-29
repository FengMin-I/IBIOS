package com.ibios.storage.sandbox;

import com.ibios.core.sandbox.WhitelistType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SandboxWhitelistRepository extends JpaRepository<SandboxWhitelistEntity, Long> {

  List<SandboxWhitelistEntity> findAllByOrderByTypeAscIdAsc();

  boolean existsByTypeAndValue(WhitelistType type, String value);
}

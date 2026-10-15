package com.ibios.memory;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, Long> {

  List<MemoryEntry> findByScope(String scope);

  @Query("SELECT m FROM MemoryEntry m WHERE m.scope = 'ARCHIVAL' ORDER BY m.id DESC")
  List<MemoryEntry> findRecentArchival(Pageable pageable);

  @Query("SELECT m FROM MemoryEntry m WHERE m.scope = 'ARCHIVAL' AND m.content LIKE %:keyword%")
  List<MemoryEntry> searchArchival(@Param("keyword") String keyword);
}

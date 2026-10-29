package com.ibios.storage.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ToolInvocationRepository extends JpaRepository<ToolInvocation, Long> {

  /**
   * 原生 SQL 而非 JPQL：这条查询跨 sessions 与 tool_invocations 两张表，写成 JPQL 会把 audit 包的 持久化单元绑到 session
   * 包的实体上，使只扫 audit 的测试切片无法启动。
   */
  @Query(
      value =
          "select distinct s.profile_name, t.tool_name from sessions s"
              + " join tool_invocations t on t.session_id = s.session_id"
              + " order by s.profile_name asc, t.tool_name asc",
      nativeQuery = true)
  List<Object[]> findProfileToolPairs();
}

package com.ibios.storage.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
  List<TaskExecution> findByTaskIdOrderByStartedAtDesc(String taskId);
}

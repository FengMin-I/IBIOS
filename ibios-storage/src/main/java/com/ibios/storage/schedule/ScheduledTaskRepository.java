package com.ibios.storage.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, String> {}

package com.ibios.web.controller;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.MemoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/memory")
public class MemoryQueryController {

  private final LongTermMemoryStore longTermMemoryStore;

  public MemoryQueryController(LongTermMemoryStore longTermMemoryStore) {
    this.longTermMemoryStore = longTermMemoryStore;
  }

  @GetMapping
  public ApiResponse<MemoryResponse> get() {
    String content = longTermMemoryStore.load();
    return ApiResponse.ok(new MemoryResponse(content));
  }
}

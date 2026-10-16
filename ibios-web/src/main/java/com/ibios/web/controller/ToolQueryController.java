package com.ibios.web.controller;

import com.ibios.core.port.ToolRegistry;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.ToolSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolQueryController {

  private final ToolRegistry toolRegistry;

  public ToolQueryController(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @GetMapping
  public ApiResponse<List<ToolSummary>> list() {
    List<ToolSummary> summaries =
        toolRegistry.listAll().stream()
            .map(t -> new ToolSummary(t.name(), t.description(), t.inputSchema()))
            .toList();
    return ApiResponse.ok(summaries);
  }
}

package com.ibios.web.controller;

import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import com.ibios.tool.sandbox.WhitelistManager;
import com.ibios.web.exception.InvalidRequestException;
import com.ibios.web.exception.ResourceNotFoundException;
import com.ibios.web.model.ApiResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sandbox/whitelist")
public class SandboxAdminController {

  private final WhitelistManager whitelistManager;

  public SandboxAdminController(WhitelistManager whitelistManager) {
    this.whitelistManager = whitelistManager;
  }

  @GetMapping
  public ApiResponse<List<WhitelistEntryView>> list() {
    return ApiResponse.ok(whitelistManager.entries());
  }

  @PostMapping
  public ApiResponse<List<WhitelistEntryView>> add(@RequestBody Map<String, String> body) {
    WhitelistType type = parseType(body.get("type"));
    String value = body.get("value");
    if (value == null || value.isBlank()) {
      throw new InvalidRequestException("value is required");
    }
    if (!whitelistManager.addEntry(type, value)) {
      throw new InvalidRequestException("entry already exists: " + value);
    }
    return ApiResponse.ok(whitelistManager.entries());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<List<WhitelistEntryView>> remove(@PathVariable Long id) {
    switch (whitelistManager.removeById(id)) {
      case DELETED:
        return ApiResponse.ok(whitelistManager.entries());
      case PROTECTED:
        throw new InvalidRequestException("工作区根不可删除");
      default:
        throw new ResourceNotFoundException("whitelist entry not found: " + id);
    }
  }

  private static WhitelistType parseType(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new InvalidRequestException("type is required");
    }
    try {
      return WhitelistType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("type must be FILE, SHELL, or HTTP");
    }
  }
}

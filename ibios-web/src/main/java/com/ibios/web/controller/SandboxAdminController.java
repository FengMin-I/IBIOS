package com.ibios.web.controller;

import com.ibios.tool.sandbox.WhitelistManager;
import com.ibios.web.model.ApiResponse;
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
  public ApiResponse<WhitelistManager.WhitelistSnapshot> query() {
    return ApiResponse.ok(whitelistManager.snapshot());
  }

  @PostMapping("/{type}")
  public ApiResponse<WhitelistManager.WhitelistSnapshot> add(
      @PathVariable String type, @RequestBody Map<String, String> body) {
    String value = body.get("value");
    if (value == null || value.isBlank()) {
      return ApiResponse.error(400, "value is required");
    }
    switch (type) {
      case "file":
        whitelistManager.addFilePath(value);
        break;
      case "shell":
        whitelistManager.addShellCommand(value);
        break;
      case "http":
        whitelistManager.addHttpDomain(value);
        break;
      default:
        return ApiResponse.error(400, "type must be file, shell, or http");
    }
    return ApiResponse.ok(whitelistManager.snapshot());
  }

  @DeleteMapping("/{type}")
  public ApiResponse<WhitelistManager.WhitelistSnapshot> remove(
      @PathVariable String type, @RequestBody Map<String, String> body) {
    String value = body.get("value");
    if (value == null || value.isBlank()) {
      return ApiResponse.error(400, "value is required");
    }
    boolean removed;
    switch (type) {
      case "file":
        removed = whitelistManager.removeFilePath(value);
        break;
      case "shell":
        removed = whitelistManager.removeShellCommand(value);
        break;
      case "http":
        removed = whitelistManager.removeHttpDomain(value);
        break;
      default:
        return ApiResponse.error(400, "type must be file, shell, or http");
    }
    if (!removed) {
      return ApiResponse.error(404, "entry not found: " + value);
    }
    return ApiResponse.ok(whitelistManager.snapshot());
  }
}

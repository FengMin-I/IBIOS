package com.ibios.tool.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.SandboxViolationException;
import com.ibios.core.tool.SearchProvider;
import com.ibios.core.tool.SearchProvider.SearchResult;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSearchToolsTest {

  @Test
  @DisplayName("DuckDuckGoSearchProvider: 返回结果格式正确（mock）")
  void duckDuckGoSearchReturnsResults() {
    SearchProvider mockProvider =
        query -> {
          List<SearchResult> results = new ArrayList<>();
          results.add(new SearchResult("Test Title", "https://example.com", "Test snippet"));
          return results;
        };

    List<SearchResult> results = mockProvider.search("test");

    assertTrue(results.size() == 1);
    assertEquals("Test Title", results.get(0).title());
    assertEquals("https://example.com", results.get(0).url());
    assertEquals("Test snippet", results.get(0).snippet());
  }

  @Test
  @DisplayName("WebSearchTool: Sandbox HTTP_REQUEST 检查")
  void webSearchSandboxEnforcement() {
    SearchProvider mockProvider = query -> List.of();
    RecordingSandbox sandbox = new RecordingSandbox();
    WebSearchTools tools = new WebSearchTools(mockProvider, sandbox);

    tools.doWebSearch("{\"query\":\"test query\"}");

    assertTrue(sandbox.lastAction != null);
    assertTrue(sandbox.lastAction.type() == ActionType.HTTP_REQUEST);
    assertTrue(sandbox.lastAction.target().contains("duckduckgo.com"));
  }

  @Test
  @DisplayName("WebSearchTool: Sandbox 拒绝时返回失败")
  void webSearchSandboxViolation() {
    SearchProvider mockProvider = query -> List.of();
    Sandbox rejectingSandbox =
        action -> {
          throw new SandboxViolationException("HTTP not allowed");
        };
    WebSearchTools tools = new WebSearchTools(mockProvider, rejectingSandbox);

    ToolExecutionResult result = tools.doWebSearch("{\"query\":\"test\"}");

    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("not allowed"));
  }

  @Test
  @DisplayName("WebSearchTool: 有结果时格式化输出")
  void webSearchWithResultsFormatted() {
    SearchProvider mockProvider =
        query ->
            List.of(
                new SearchResult("Title A", "https://a.com", "Snippet A"),
                new SearchResult("Title B", "https://b.com", "Snippet B"));
    Sandbox sandbox = action -> {};
    WebSearchTools tools = new WebSearchTools(mockProvider, sandbox);

    ToolExecutionResult result = tools.doWebSearch("{\"query\":\"test\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("1. Title A"));
    assertTrue(result.output().contains("https://a.com"));
    assertTrue(result.output().contains("Snippet A"));
    assertTrue(result.output().contains("2. Title B"));
  }

  @Test
  @DisplayName("WebSearchTool: 无结果时返回提示")
  void webSearchNoResults() {
    SearchProvider mockProvider = query -> List.of();
    Sandbox sandbox = action -> {};
    WebSearchTools tools = new WebSearchTools(mockProvider, sandbox);

    ToolExecutionResult result = tools.doWebSearch("{\"query\":\"test\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("No results found"));
  }

  private static class RecordingSandbox implements Sandbox {
    SandboxAction lastAction;

    @Override
    public void enforce(SandboxAction action) {
      lastAction = action;
    }
  }
}

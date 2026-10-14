package com.ibios.tool.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.SearchProvider;
import com.ibios.core.tool.SearchProvider.SearchResult;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTools {

  private static final Logger log = LoggerFactory.getLogger(WebSearchTools.class);

  private final SearchProvider searchProvider;
  private final Sandbox sandbox;
  private final ObjectMapper objectMapper;

  public WebSearchTools(SearchProvider searchProvider, Sandbox sandbox) {
    this.searchProvider = searchProvider;
    this.sandbox = sandbox;
    this.objectMapper = new ObjectMapper();
  }

  @Component("webSearchTool")
  public static final class WebSearchTool implements IbiosTool {

    private final WebSearchTools delegate;

    public WebSearchTool(WebSearchTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "web_search";
    }

    @Override
    public String description() {
      return "Search the web using DuckDuckGo and return a list of results";
    }

    @Override
    public String inputSchema() {
      return """
          {
            "type": "object",
            "properties": {
              "query": {"type": "string", "description": "Search query"}
            },
            "required": ["query"]
          }
          """;
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doWebSearch(argumentsJson);
    }
  }

  ToolExecutionResult doWebSearch(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String query = args.get("query").asText();

      sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, "duckduckgo.com"));

      log.info("web_search: {}", query);
      List<SearchResult> results = searchProvider.search(query);

      if (results.isEmpty()) {
        return ToolExecutionResult.success("No results found");
      }

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < results.size(); i++) {
        SearchResult r = results.get(i);
        sb.append(i + 1).append(". ").append(r.title()).append("\n");
        sb.append("   URL: ").append(r.url()).append("\n");
        sb.append("   ").append(r.snippet()).append("\n\n");
      }

      return ToolExecutionResult.success(sb.toString());
    } catch (Exception e) {
      return ToolExecutionResult.failure("Web search failed: " + e.getMessage(), true);
    }
  }
}

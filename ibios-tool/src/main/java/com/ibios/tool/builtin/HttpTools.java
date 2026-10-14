package com.ibios.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpTools {

  private static final Logger log = LoggerFactory.getLogger(HttpTools.class);

  private final Sandbox sandbox;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public HttpTools(Sandbox sandbox, RestClient.Builder restClientBuilder) {
    this.sandbox = sandbox;
    this.restClient = restClientBuilder.build();
    this.objectMapper = new ObjectMapper();
  }

  @Component("httpGetTool")
  static final class HttpGetTool implements IbiosTool {

    private final HttpTools delegate;

    HttpGetTool(HttpTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "http_get";
    }

    @Override
    public String description() {
      return "Send an HTTP GET request to the given URL and return the response body";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"url":{"type":"string","description":"The URL to request"}\
          },"required":["url"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doGet(argumentsJson);
    }
  }

  @Component("httpPostTool")
  static final class HttpPostTool implements IbiosTool {

    private final HttpTools delegate;

    HttpPostTool(HttpTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "http_post";
    }

    @Override
    public String description() {
      return "Send an HTTP POST request to the given URL with a JSON body and return the response";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"url":{"type":"string","description":"The URL to request"}\
          ,"body":{"type":"string","description":"JSON body to send"}},"required":["url"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doPost(argumentsJson);
    }
  }

  ToolExecutionResult doGet(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String url = args.get("url").asText();
      sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, url));

      String response = restClient.get().uri(url).retrieve().body(String.class);
      log.info("http_get: {}", url);
      return ToolExecutionResult.success(response != null ? response : "");
    } catch (Exception e) {
      return ToolExecutionResult.failure("HTTP GET failed: " + e.getMessage(), true);
    }
  }

  ToolExecutionResult doPost(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String url = args.get("url").asText();
      sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, url));

      String body = args.has("body") ? args.get("body").asText() : "";
      String response =
          restClient
              .post()
              .uri(url)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      log.info("http_post: {}", url);
      return ToolExecutionResult.success(response != null ? response : "");
    } catch (Exception e) {
      return ToolExecutionResult.failure("HTTP POST failed: " + e.getMessage(), true);
    }
  }
}

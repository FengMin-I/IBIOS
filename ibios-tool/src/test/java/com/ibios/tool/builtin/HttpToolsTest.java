package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxViolationException;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.HashSet;
import java.util.Set;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpToolsTest {

  private MockWebServer mockWebServer;
  private HttpTools httpTools;
  private TestSandbox sandbox;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    sandbox = new TestSandbox();
    httpTools = new HttpTools(sandbox, RestClient.builder());
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void httpGetShouldReturnResponseBody() {
    mockWebServer.enqueue(new MockResponse().setBody("hello").setResponseCode(200));
    String url = mockWebServer.url("/test").toString();
    sandbox.allowDomain(url);

    ToolExecutionResult result = httpTools.doGet("{\"url\":\"" + url + "\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("hello"));
  }

  @Test
  void httpPostShouldSendBodyAndReturnResponse() {
    mockWebServer.enqueue(new MockResponse().setBody("ok").setResponseCode(200));
    String url = mockWebServer.url("/api").toString();
    sandbox.allowDomain(url);

    ToolExecutionResult result =
        httpTools.doPost("{\"url\":\"" + url + "\",\"body\":\"{\\\"key\\\":\\\"value\\\"}\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("ok"));
  }

  @Test
  void httpGetOutsideAllowedDomainShouldFail() {
    String url = mockWebServer.url("/blocked").toString();
    // Don't allow the domain in sandbox

    ToolExecutionResult result = httpTools.doGet("{\"url\":\"" + url + "\"}");

    assertFalse(result.success());
    assertTrue(
        result.errorMessage().contains("not allowed") || result.errorMessage().contains("Sandbox"));
  }

  private static class TestSandbox implements Sandbox {
    private final Set<String> allowedDomains = new HashSet<>();

    void allowDomain(String url) {
      allowedDomains.add(url);
    }

    @Override
    public void enforce(SandboxAction action) {
      String target = action.target();
      boolean allowed = allowedDomains.stream().anyMatch(target::startsWith);
      if (!allowed) {
        throw new SandboxViolationException("Domain not allowed: " + target);
      }
    }
  }
}

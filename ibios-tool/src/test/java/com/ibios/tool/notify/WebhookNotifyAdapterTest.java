package com.ibios.tool.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.notify.NotifyTarget;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class WebhookNotifyAdapterTest {

  private MockWebServer mockWebServer;
  private WebhookNotifyAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    RestClient.Builder builder =
        RestClient.builder()
            .messageConverters(
                converters -> converters.add(new MappingJackson2HttpMessageConverter()));
    adapter = new WebhookNotifyAdapter(builder);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("发送后 POST body 带 content")
  void postBodyShouldContainContent() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/webhook").toString();
    NotifyTarget target = new NotifyTarget("webhook", Map.of("url", url));

    adapter.send(target, "hello world");

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("\"msg_type\":\"text\""));
    assertTrue(body.contains("\"text\":\"hello world\""));
  }

  @Test
  @DisplayName("URL 来自 NotifyTarget.config 而不是硬编码")
  void urlShouldComeFromTargetConfig() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/custom-path").toString();
    NotifyTarget target = new NotifyTarget("webhook", Map.of("url", url));

    adapter.send(target, "test");

    RecordedRequest request = mockWebServer.takeRequest();
    assertTrue(request.getPath().contains("/custom-path"));
  }

  @Test
  @DisplayName("webhook 返回 5xx 时异常向上抛、不静默吞掉")
  void serverErrorShouldThrowException() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    String url = mockWebServer.url("/webhook").toString();
    NotifyTarget target = new NotifyTarget("webhook", Map.of("url", url));

    assertThrows(HttpServerErrorException.class, () -> adapter.send(target, "should fail"));
  }
}

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

class DingTalkNotifyAdapterTest {

  private MockWebServer mockWebServer;
  private DingTalkNotifyAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    RestClient.Builder builder =
        RestClient.builder()
            .messageConverters(
                converters -> converters.add(new MappingJackson2HttpMessageConverter()));
    adapter = new DingTalkNotifyAdapter(builder);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("钉钉格式：body 含 msgtype=text 和 text.content")
  void postBodyShouldMatchDingTalkFormat() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/robot/send").toString();
    NotifyTarget target = new NotifyTarget("dingtalk", Map.of("url", url));

    adapter.send(target, "hello dingtalk");

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("\"msgtype\""));
    assertTrue(body.contains("\"text\""));
    assertTrue(body.contains("hello dingtalk"));
  }

  @Test
  @DisplayName("钉钉：URL 来自 NotifyTarget.config")
  void urlShouldComeFromTargetConfig() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/robot/send?access_token=TOKEN").toString();
    NotifyTarget target = new NotifyTarget("dingtalk", Map.of("url", url));

    adapter.send(target, "test");

    RecordedRequest request = mockWebServer.takeRequest();
    assertTrue(request.getPath().contains("/robot/send"));
  }

  @Test
  @DisplayName("钉钉：5xx 时异常向上抛")
  void serverErrorShouldThrowException() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    String url = mockWebServer.url("/robot/send").toString();
    NotifyTarget target = new NotifyTarget("dingtalk", Map.of("url", url));

    assertThrows(HttpServerErrorException.class, () -> adapter.send(target, "should fail"));
  }
}

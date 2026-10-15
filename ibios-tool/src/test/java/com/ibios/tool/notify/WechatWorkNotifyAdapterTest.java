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

class WechatWorkNotifyAdapterTest {

  private MockWebServer mockWebServer;
  private WechatWorkNotifyAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    RestClient.Builder builder =
        RestClient.builder()
            .messageConverters(
                converters -> converters.add(new MappingJackson2HttpMessageConverter()));
    adapter = new WechatWorkNotifyAdapter(builder);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("企业微信格式：body 含 msgtype=text 和 text.content")
  void postBodyShouldMatchWechatWorkFormat() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/cgi-bin/webhook/send").toString();
    NotifyTarget target = new NotifyTarget("wechat_work", Map.of("url", url));

    adapter.send(target, "hello wechat");

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("\"msgtype\""));
    assertTrue(body.contains("\"text\""));
    assertTrue(body.contains("hello wechat"));
  }

  @Test
  @DisplayName("企业微信：URL 来自 NotifyTarget.config")
  void urlShouldComeFromTargetConfig() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String url = mockWebServer.url("/cgi-bin/webhook/send?key=KEY").toString();
    NotifyTarget target = new NotifyTarget("wechat_work", Map.of("url", url));

    adapter.send(target, "test");

    RecordedRequest request = mockWebServer.takeRequest();
    assertTrue(request.getPath().contains("/cgi-bin/webhook/send"));
  }

  @Test
  @DisplayName("企业微信：5xx 时异常向上抛")
  void serverErrorShouldThrowException() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    String url = mockWebServer.url("/cgi-bin/webhook/send").toString();
    NotifyTarget target = new NotifyTarget("wechat_work", Map.of("url", url));

    assertThrows(HttpServerErrorException.class, () -> adapter.send(target, "should fail"));
  }
}

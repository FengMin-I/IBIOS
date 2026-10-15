package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

class Mem0MemoryStoreTest {

  private MockWebServer mockWebServer;
  private Mem0MemoryStore store;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    RestClient restClient =
        RestClient.builder()
            .messageConverters(
                converters -> converters.add(new MappingJackson2HttpMessageConverter()))
            .build();
    store = new Mem0MemoryStore(restClient, mockWebServer.url("/").toString());
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("append 请求体带 scope metadata")
  void appendBodyShouldContainScope() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    store.append("用户偏好简洁", com.ibios.core.memory.MemoryScope.CORE);

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("/v1/memories/"));
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("CORE"));
    assertTrue(body.contains("用户偏好简洁"));
  }

  @Test
  @DisplayName("recall 转发查询到 search 端点")
  void recallShouldForwardQuery() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[{\"memory\": \"匹配的记忆1\"}, {\"memory\": \"匹配的记忆2\"}]"));

    List<String> results = store.recallByKeyword("偏好");

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("/v1/memories/search/"));
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("偏好"));
    assertEquals(2, results.size());
    assertTrue(results.get(0).contains("匹配的记忆1"));
  }

  @Test
  @DisplayName("recall 空结果返回空列表")
  void recallEmptyReturnsEmptyList() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[]"));

    List<String> results = store.recallByKeyword("不存在");
    assertTrue(results.isEmpty());
  }
}

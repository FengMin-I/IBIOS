package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class Mem0MemoryStore implements LongTermMemoryStore {

  private final RestClient restClient;
  private final String baseUrl;

  public Mem0MemoryStore(RestClient restClient, String baseUrl) {
    this.restClient = restClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public void append(String content, MemoryScope scope) {
    // TODO: Sandbox.enforce 调用位——24 节接线
    Map<String, Object> body =
        Map.of(
            "messages", List.of(Map.of("role", "user", "content", content)),
            "metadata", Map.of("scope", scope.name()));
    restClient
        .post()
        .uri(baseUrl + "/v1/memories/")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public String load() {
    // TODO: Sandbox.enforce 调用位——24 节接线
    String response = restClient.get().uri(baseUrl + "/v1/memories/").retrieve().body(String.class);
    return response != null ? response : "";
  }

  @Override
  public List<String> recallByKeyword(String keyword) {
    // TODO: Sandbox.enforce 调用位——24 节接线
    Map<String, Object> body = Map.of("query", keyword);
    Object response =
        restClient
            .post()
            .uri(baseUrl + "/v1/memories/search/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Object.class);
    if (response instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results = (List<Map<String, Object>>) response;
      return results.stream().map(r -> String.valueOf(r.getOrDefault("memory", ""))).toList();
    }
    return Collections.emptyList();
  }
}

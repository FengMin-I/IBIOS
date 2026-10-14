package com.ibios.tool.search;

import com.ibios.core.tool.SearchProvider;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/** DuckDuckGo Instant Answer API 实现。免 API key，返回即时答案和相关主题。 */
public class DuckDuckGoSearchProvider implements SearchProvider {

  private static final Logger log = LoggerFactory.getLogger(DuckDuckGoSearchProvider.class);
  private static final String BASE_URL = "https://api.duckduckgo.com/";
  private static final int MAX_RESULTS = 10;

  private final RestClient restClient;

  public DuckDuckGoSearchProvider(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public List<SearchResult> search(String query) {
    try {
      String url =
          BASE_URL
              + "?q="
              + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
              + "&format=json&no_html=1&skip_disambig=1";
      String response = restClient.get().uri(url).retrieve().body(String.class);

      if (response == null || response.isBlank()) {
        return List.of();
      }

      return parseResponse(response);
    } catch (Exception e) {
      log.warn("DuckDuckGo search failed for query '{}': {}", query, e.getMessage());
      return List.of();
    }
  }

  private List<SearchResult> parseResponse(String json) {
    List<SearchResult> results = new ArrayList<>();

    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

      // Extract Abstract (main answer)
      com.fasterxml.jackson.databind.JsonNode abstractNode = root.get("Abstract");
      com.fasterxml.jackson.databind.JsonNode abstractUrlNode = root.get("AbstractURL");
      if (abstractNode != null && !abstractNode.asText().isBlank()) {
        String title = root.has("Heading") ? root.get("Heading").asText() : "Result";
        String url = abstractUrlNode != null ? abstractUrlNode.asText() : "";
        String snippet = abstractNode.asText();
        results.add(new SearchResult(title, url, snippet));
      }

      // Extract RelatedTopics
      com.fasterxml.jackson.databind.JsonNode relatedTopics = root.get("RelatedTopics");
      if (relatedTopics != null && relatedTopics.isArray()) {
        for (com.fasterxml.jackson.databind.JsonNode topic : relatedTopics) {
          if (results.size() >= MAX_RESULTS) break;

          if (topic.has("Text") && topic.has("FirstURL")) {
            String text = topic.get("Text").asText();
            String topicUrl = topic.get("FirstURL").asText();

            // Split text into title and snippet (first sentence is title)
            int dotIndex = text.indexOf('.');
            String title = dotIndex > 0 ? text.substring(0, dotIndex) : text;
            String snippet = text;

            results.add(new SearchResult(title, topicUrl, snippet));
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse DuckDuckGo response: {}", e.getMessage());
    }

    return results;
  }
}

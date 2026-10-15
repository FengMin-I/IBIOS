package com.ibios.tool.notify;

import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyTarget;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Slack Incoming Webhook 适配器。按 Slack 约定格式包装消息：{"text":"..."}。 */
@Component
public class SlackNotifyAdapter implements NotifyChannelAdapter {

  private final RestClient restClient;

  public SlackNotifyAdapter(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public void send(NotifyTarget target, String content) {
    String url = target.config().get("url");
    restClient
        .post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("text", content))
        .retrieve()
        .toBodilessEntity();
  }
}

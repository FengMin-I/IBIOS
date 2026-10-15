package com.ibios.tool.notify;

import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyTarget;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 钉钉自定义机器人适配器。按钉钉约定格式包装消息：{"msgtype":"text","text":{"content":"..."}}。 */
@Component
public class DingTalkNotifyAdapter implements NotifyChannelAdapter {

  private final RestClient restClient;

  public DingTalkNotifyAdapter(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public void send(NotifyTarget target, String content) {
    String url = target.config().get("url");
    Map<String, Object> body = Map.of("msgtype", "text", "text", Map.of("content", content));
    restClient
        .post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }
}

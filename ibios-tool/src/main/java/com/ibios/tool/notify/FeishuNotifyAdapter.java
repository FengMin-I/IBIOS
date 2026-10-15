package com.ibios.tool.notify;

import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyTarget;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 飞书/Lark 自定义机器人适配器。按飞书约定格式包装消息：{"msg_type":"text","content":{"text":"..."}}。 */
@Component
public class FeishuNotifyAdapter implements NotifyChannelAdapter {

  private final RestClient restClient;

  public FeishuNotifyAdapter(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  @Override
  public void send(NotifyTarget target, String content) {
    String url = target.config().get("url");
    Map<String, Object> body = Map.of("msg_type", "text", "content", Map.of("text", content));
    restClient
        .post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }
}

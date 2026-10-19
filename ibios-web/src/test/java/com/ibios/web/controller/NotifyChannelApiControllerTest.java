package com.ibios.web.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.notify.NotifyChannelStore;
import com.ibios.core.notify.NotifyChannelView;
import com.ibios.web.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("NotifyChannelApiController 测试")
class NotifyChannelApiControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private NotifyChannelStore store;

  @BeforeEach
  void setUp() {
    store = Mockito.mock(NotifyChannelStore.class);
    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new NotifyChannelApiController(store))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET 返回渠道列表")
  void list_returnsChannels() throws Exception {
    when(store.listAll())
        .thenReturn(
            List.of(new NotifyChannelView(1L, "team-webhook", "webhook", "https://x", "团队群")));

    mockMvc
        .perform(get("/api/v1/notify-channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].name").value("team-webhook"))
        .andExpect(jsonPath("$.data[0].type").value("webhook"));
  }

  @Test
  @DisplayName("POST 创建渠道并返回 id")
  void create_returnsCreatedChannel() throws Exception {
    when(store.create("team-webhook", "webhook", "https://x", "团队群"))
        .thenReturn(new NotifyChannelView(7L, "team-webhook", "webhook", "https://x", "团队群"));

    mockMvc
        .perform(
            post("/api/v1/notify-channels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "team-webhook",
                            "type", "webhook",
                            "url", "https://x",
                            "description", "团队群"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(7))
        .andExpect(jsonPath("$.data.name").value("team-webhook"));

    verify(store).create("team-webhook", "webhook", "https://x", "团队群");
  }

  @Test
  @DisplayName("POST 重名渠道返回 400")
  void create_duplicateName_returns400() throws Exception {
    when(store.create("dup", "webhook", "https://x", null))
        .thenThrow(new IllegalArgumentException("Notify channel already exists: dup"));

    mockMvc
        .perform(
            post("/api/v1/notify-channels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "dup", "type", "webhook", "url", "https://x"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("PUT 更新渠道")
  void update_returnsUpdatedChannel() throws Exception {
    when(store.update(3L, "team-lark", "feishu", "https://y", "新群"))
        .thenReturn(new NotifyChannelView(3L, "team-lark", "feishu", "https://y", "新群"));

    mockMvc
        .perform(
            put("/api/v1/notify-channels/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "team-lark",
                            "type", "feishu",
                            "url", "https://y",
                            "description", "新群"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("team-lark"))
        .andExpect(jsonPath("$.data.type").value("feishu"));
  }

  @Test
  @DisplayName("PUT 不存在的渠道返回 404")
  void update_missing_returns404() throws Exception {
    when(store.update(anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(null);

    mockMvc
        .perform(
            put("/api/v1/notify-channels/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "x", "type", "webhook", "url", "https://x"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("DELETE 删除渠道")
  void delete_removesChannel() throws Exception {
    when(store.getById(5L))
        .thenReturn(new NotifyChannelView(5L, "old", "webhook", "https://x", ""));

    mockMvc
        .perform(delete("/api/v1/notify-channels/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    verify(store).delete(5L);
  }

  @Test
  @DisplayName("DELETE 不存在的渠道返回 404")
  void delete_missing_returns404() throws Exception {
    when(store.getById(404L)).thenReturn(null);

    mockMvc
        .perform(delete("/api/v1/notify-channels/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }
}

package com.ibios.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.provider.ProviderView;
import com.ibios.provider.ProviderAdminService;
import com.ibios.provider.ProviderInUseException;
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

@DisplayName("ProviderApiController 测试")
class ProviderApiControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private ProviderAdminService service;

  @BeforeEach
  void setUp() {
    service = Mockito.mock(ProviderAdminService.class);
    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProviderApiController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET 列表返回脱敏 key")
  void list_returnsMaskedKeys() throws Exception {
    when(service.list())
        .thenReturn(
            List.of(
                new ProviderView(1L, "deepseek", "openai", "sk-****f847", null, "deepseek-chat")));

    mockMvc
        .perform(get("/api/v1/providers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("deepseek"))
        .andExpect(jsonPath("$.data[0].type").value("openai"))
        .andExpect(jsonPath("$.data[0].apiKey").value("sk-****f847"));
  }

  @Test
  @DisplayName("GET 详情返回明文 key")
  void detail_returnsPlaintextKey() throws Exception {
    when(service.get(1L))
        .thenReturn(new ProviderView(1L, "deepseek", "openai", "sk-plain", null, "deepseek-chat"));

    mockMvc
        .perform(get("/api/v1/providers/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.apiKey").value("sk-plain"));
  }

  @Test
  @DisplayName("GET 详情不存在返回 404")
  void detail_missing_returns404() throws Exception {
    when(service.get(99L)).thenReturn(null);

    mockMvc
        .perform(get("/api/v1/providers/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("POST 创建 Provider")
  void create_returnsCreatedProvider() throws Exception {
    when(service.create("qwen", "dashscope", "sk-1", null, "qwen-plus"))
        .thenReturn(new ProviderView(7L, "qwen", "dashscope", "sk-1", null, "qwen-plus"));

    mockMvc
        .perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "qwen",
                            "type", "dashscope",
                            "apiKey", "sk-1",
                            "model", "qwen-plus"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(7))
        .andExpect(jsonPath("$.data.name").value("qwen"));

    verify(service).create("qwen", "dashscope", "sk-1", null, "qwen-plus");
  }

  @Test
  @DisplayName("POST 重名返回 400")
  void create_duplicate_returns400() throws Exception {
    when(service.create(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Provider already exists: qwen"));

    mockMvc
        .perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("name", "qwen", "type", "dashscope"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("PUT 更新 Provider")
  void update_returnsUpdatedProvider() throws Exception {
    when(service.update(3L, "openai", "sk-2", "https://api.example.com", "gpt-4o"))
        .thenReturn(
            new ProviderView(3L, "custom", "openai", "sk-2", "https://api.example.com", "gpt-4o"));

    mockMvc
        .perform(
            put("/api/v1/providers/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "type", "openai",
                            "apiKey", "sk-2",
                            "baseUrl", "https://api.example.com",
                            "model", "gpt-4o"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.type").value("openai"))
        .andExpect(jsonPath("$.data.model").value("gpt-4o"));
  }

  @Test
  @DisplayName("PUT 不存在返回 404")
  void update_missing_returns404() throws Exception {
    when(service.update(anyLong(), any(), any(), any(), any())).thenReturn(null);

    mockMvc
        .perform(
            put("/api/v1/providers/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("type", "openai"))))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE 被 Agent 引用时返回 409")
  void delete_referenced_returns409() throws Exception {
    when(service.get(5L))
        .thenReturn(new ProviderView(5L, "deepseek", "openai", "sk-1", null, "deepseek-chat"));
    doThrow(new ProviderInUseException("deepseek", "daily-news")).when(service).delete(5L);

    mockMvc
        .perform(delete("/api/v1/providers/5"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(409))
        .andExpect(
            jsonPath("$.message").value("Provider deepseek is still used by agent: daily-news"));
  }

  @Test
  @DisplayName("DELETE 不存在返回 404")
  void delete_missing_returns404() throws Exception {
    when(service.get(404L)).thenReturn(null);

    mockMvc.perform(delete("/api/v1/providers/404")).andExpect(status().isNotFound());
  }
}

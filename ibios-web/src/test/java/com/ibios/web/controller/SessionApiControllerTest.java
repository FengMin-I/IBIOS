package com.ibios.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import com.ibios.web.exception.GlobalExceptionHandler;
import com.ibios.web.model.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionApiControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private AgentService agentService;
  private SessionManager sessionManager;

  @BeforeEach
  void setUp() {
    agentService = Mockito.mock(AgentService.class);
    sessionManager = Mockito.mock(SessionManager.class);
    ProfileRegistry profileRegistry = Mockito.mock(ProfileRegistry.class);
    objectMapper = new ObjectMapper();

    SessionApiController controller =
        new SessionApiController(agentService, sessionManager, profileRegistry);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("消息超 32KB 返回 400")
  void messageExceeding32KB_returns400() throws Exception {
    String largeContent = "x".repeat(32 * 1024 + 1);
    SendMessageRequest request = new SendMessageRequest(largeContent);

    mockMvc
        .perform(
            post("/api/v1/sessions/s-1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("Session 不存在返回 404")
  void sessionNotFound_returns404() throws Exception {
    when(sessionManager.get("nonexistent")).thenReturn(null);
    SendMessageRequest request = new SendMessageRequest("hello");

    mockMvc
        .perform(
            post("/api/v1/sessions/nonexistent/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("正常请求 agentService.process 恰被调一次")
  void validRequest_agentServiceCalledOnce() throws Exception {
    Session session = new Session("s-1", "test-profile", "web", "user1");
    when(sessionManager.get("s-1")).thenReturn(session);
    when(agentService.process(any(Session.class), eq("hello"))).thenReturn("Hi there");
    SendMessageRequest request = new SendMessageRequest("hello");

    mockMvc
        .perform(
            post("/api/v1/sessions/s-1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reply").value("Hi there"));

    verify(agentService).process(any(Session.class), eq("hello"));
  }
}

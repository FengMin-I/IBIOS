package com.ibios.web.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibios.web.model.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("IllegalArgumentException 返回 400")
  void illegalArgumentException_returns400() throws Exception {
    mockMvc
        .perform(get("/test/bad-request"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("bad param"));
  }

  @Test
  @DisplayName("InvalidRequestException 返回 400")
  void invalidRequestException_returns400() throws Exception {
    mockMvc
        .perform(get("/test/invalid-request"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("SessionNotFoundException 返回 404")
  void sessionNotFoundException_returns404() throws Exception {
    mockMvc
        .perform(get("/test/session-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("AgentNotFoundException 返回 404")
  void agentNotFoundException_returns404() throws Exception {
    mockMvc
        .perform(get("/test/agent-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("AgentTimeoutException 返回 504")
  void agentTimeoutException_returns504() throws Exception {
    mockMvc
        .perform(get("/test/timeout"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value(504));
  }

  @Test
  @DisplayName("IllegalStateException 返回 503")
  void illegalStateException_returns503() throws Exception {
    mockMvc
        .perform(get("/test/service-unavailable"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(503));
  }

  @Test
  @DisplayName("未知异常返回 500，响应体不含内部异常细节")
  void unknownException_returns500WithoutLeakingDetails() throws Exception {
    mockMvc
        .perform(get("/test/internal-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(500))
        .andExpect(jsonPath("$.message").value("Internal server error"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("jdbc:sqlite"))));
  }

  @Test
  @DisplayName("所有错误响应都是统一 ApiResponse 信封")
  void allErrorsReturnApiResponseEnvelope() throws Exception {
    mockMvc
        .perform(get("/test/bad-request"))
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @RestController
  @RequestMapping("/test")
  static class TestController {

    @GetMapping("/bad-request")
    public ApiResponse<Void> badRequest() {
      throw new IllegalArgumentException("bad param");
    }

    @GetMapping("/invalid-request")
    public ApiResponse<Void> invalidRequest() {
      throw new InvalidRequestException("invalid");
    }

    @GetMapping("/session-not-found")
    public ApiResponse<Void> sessionNotFound() {
      throw new SessionNotFoundException("s-999");
    }

    @GetMapping("/agent-not-found")
    public ApiResponse<Void> agentNotFound() {
      throw new AgentNotFoundException("unknown-agent");
    }

    @GetMapping("/timeout")
    public ApiResponse<Void> timeout() {
      throw new AgentTimeoutException("Agent call timed out");
    }

    @GetMapping("/service-unavailable")
    public ApiResponse<Void> serviceUnavailable() {
      throw new IllegalStateException("Provider connection failed");
    }

    @GetMapping("/internal-error")
    public ApiResponse<Void> internalError() {
      throw new RuntimeException("jdbc:sqlite:/data/ibios.db connect failed");
    }
  }
}

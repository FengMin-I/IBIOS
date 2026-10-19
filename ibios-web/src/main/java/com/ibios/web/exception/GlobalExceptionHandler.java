package com.ibios.web.exception;

import com.ibios.web.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler({IllegalArgumentException.class, InvalidRequestException.class})
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(400, ex.getMessage()));
  }

  @ExceptionHandler({
    SessionNotFoundException.class,
    AgentNotFoundException.class,
    ResourceNotFoundException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(404, ex.getMessage()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(404, "Resource not found: " + ex.getResourcePath()));
  }

  @ExceptionHandler({ServiceUnavailableException.class, IllegalStateException.class})
  public ResponseEntity<ApiResponse<Void>> handleServiceUnavailable(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error(503, ex.getMessage()));
  }

  @ExceptionHandler(NonTransientAiException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiException(NonTransientAiException ex) {
    log.warn("AI service error: {}", ex.getMessage());
    String message = "LLM 服务调用失败";
    if (ex.getMessage() != null && ex.getMessage().contains("API-key")) {
      message = "LLM API Key 未配置，请设置环境变量后重试";
    }
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error(503, message));
  }

  @ExceptionHandler(AgentTimeoutException.class)
  public ResponseEntity<ApiResponse<Void>> handleTimeout(AgentTimeoutException ex) {
    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
        .body(ApiResponse.error(504, ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(500, "Internal server error"));
  }
}

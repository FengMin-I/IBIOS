package com.ibios.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private final int code;
  private final String message;
  private final T data;
  private final Instant timestamp;

  private ApiResponse(int code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.timestamp = Instant.now();
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "success", data);
  }

  public static <T> ApiResponse<T> ok() {
    return new ApiResponse<>(0, "success", null);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }

  public Instant getTimestamp() {
    return timestamp;
  }
}

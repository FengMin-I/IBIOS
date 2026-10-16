package com.ibios.web.controller;

import com.ibios.provider.ProvidersProperties;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.HealthResponse;
import com.ibios.web.model.InfoResponse;
import com.ibios.web.model.InfoResponse.ProviderStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SystemController {

  private final ProvidersProperties providersProperties;

  public SystemController(ProvidersProperties providersProperties) {
    this.providersProperties = providersProperties;
  }

  @GetMapping("/health")
  public ApiResponse<HealthResponse> health() {
    return ApiResponse.ok(new HealthResponse("ok"));
  }

  @GetMapping("/info")
  public ApiResponse<InfoResponse> info() {
    List<ProviderStatus> providers =
        providersProperties.getProviders().stream()
            .map(
                p ->
                    new ProviderStatus(
                        p.getName(), p.getApiKey() != null && !p.getApiKey().isEmpty()))
            .toList();
    return ApiResponse.ok(new InfoResponse("0.0.1-SNAPSHOT", providers));
  }
}

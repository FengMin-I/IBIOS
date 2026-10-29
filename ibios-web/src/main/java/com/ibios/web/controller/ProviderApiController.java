package com.ibios.web.controller;

import com.ibios.core.provider.ProviderView;
import com.ibios.provider.ProviderAdminService;
import com.ibios.web.exception.ResourceNotFoundException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.ProviderRequest;
import com.ibios.web.model.ProviderResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderApiController {

  private final ProviderAdminService service;

  public ProviderApiController(ProviderAdminService service) {
    this.service = service;
  }

  /** 列表返回脱敏 key；详情（GET /{id}）返回明文，供编辑页回填。 */
  @GetMapping
  public ApiResponse<List<ProviderResponse>> list() {
    List<ProviderResponse> providers = service.list().stream().map(this::toResponse).toList();
    return ApiResponse.ok(providers);
  }

  @GetMapping("/{id}")
  public ApiResponse<ProviderResponse> get(@PathVariable Long id) {
    ProviderView view = service.get(id);
    if (view == null) {
      throw new ResourceNotFoundException("Provider not found: " + id);
    }
    return ApiResponse.ok(toResponse(view));
  }

  @PostMapping
  public ApiResponse<ProviderResponse> create(@RequestBody ProviderRequest request) {
    ProviderView created =
        service.create(
            request.getName(),
            request.getType(),
            request.getApiKey(),
            request.getBaseUrl(),
            request.getModel());
    return ApiResponse.ok(toResponse(created));
  }

  @PutMapping("/{id}")
  public ApiResponse<ProviderResponse> update(
      @PathVariable Long id, @RequestBody ProviderRequest request) {
    ProviderView updated =
        service.update(
            id, request.getType(), request.getApiKey(), request.getBaseUrl(), request.getModel());
    if (updated == null) {
      throw new ResourceNotFoundException("Provider not found: " + id);
    }
    return ApiResponse.ok(toResponse(updated));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    if (service.get(id) == null) {
      throw new ResourceNotFoundException("Provider not found: " + id);
    }
    service.delete(id);
    return ApiResponse.ok();
  }

  private ProviderResponse toResponse(ProviderView view) {
    ProviderResponse response = new ProviderResponse();
    response.setId(view.id());
    response.setName(view.name());
    response.setType(view.type());
    response.setApiKey(view.apiKey());
    response.setBaseUrl(view.baseUrl());
    response.setModel(view.model());
    return response;
  }
}

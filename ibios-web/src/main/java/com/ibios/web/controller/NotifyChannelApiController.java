package com.ibios.web.controller;

import com.ibios.core.notify.NotifyChannelStore;
import com.ibios.core.notify.NotifyChannelView;
import com.ibios.web.exception.ResourceNotFoundException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.NotifyChannelRequest;
import com.ibios.web.model.NotifyChannelResponse;
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
@RequestMapping("/api/v1/notify-channels")
public class NotifyChannelApiController {

  private final NotifyChannelStore store;

  public NotifyChannelApiController(NotifyChannelStore store) {
    this.store = store;
  }

  @GetMapping
  public ApiResponse<List<NotifyChannelResponse>> list() {
    List<NotifyChannelResponse> channels = store.listAll().stream().map(this::toResponse).toList();
    return ApiResponse.ok(channels);
  }

  @PostMapping
  public ApiResponse<NotifyChannelResponse> create(@RequestBody NotifyChannelRequest request) {
    NotifyChannelView created =
        store.create(
            request.getName(), request.getType(), request.getUrl(), request.getDescription());
    return ApiResponse.ok(toResponse(created));
  }

  @PutMapping("/{id}")
  public ApiResponse<NotifyChannelResponse> update(
      @PathVariable Long id, @RequestBody NotifyChannelRequest request) {
    NotifyChannelView updated =
        store.update(
            id, request.getName(), request.getType(), request.getUrl(), request.getDescription());
    if (updated == null) {
      throw new ResourceNotFoundException("Notify channel not found: " + id);
    }
    return ApiResponse.ok(toResponse(updated));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    if (store.getById(id) == null) {
      throw new ResourceNotFoundException("Notify channel not found: " + id);
    }
    store.delete(id);
    return ApiResponse.ok();
  }

  private NotifyChannelResponse toResponse(NotifyChannelView view) {
    NotifyChannelResponse response = new NotifyChannelResponse();
    response.setId(view.id());
    response.setName(view.name());
    response.setType(view.type());
    response.setUrl(view.url());
    response.setDescription(view.description());
    return response;
  }
}

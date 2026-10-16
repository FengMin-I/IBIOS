package com.ibios.web.controller;

import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import com.ibios.web.exception.InvalidRequestException;
import com.ibios.web.exception.SessionNotFoundException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.CreateSessionRequest;
import com.ibios.web.model.MessageResponse;
import com.ibios.web.model.SendMessageRequest;
import com.ibios.web.model.SessionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionApiController {

  private static final int MAX_MESSAGE_SIZE = 32 * 1024;
  private static final int MAX_HISTORY_SIZE = 100;

  private final AgentService agentService;
  private final SessionManager sessionManager;
  private final ProfileRegistry profileRegistry;

  public SessionApiController(
      AgentService agentService, SessionManager sessionManager, ProfileRegistry profileRegistry) {
    this.agentService = agentService;
    this.sessionManager = sessionManager;
    this.profileRegistry = profileRegistry;
  }

  @PostMapping
  public ApiResponse<SessionResponse> create(@RequestBody CreateSessionRequest request) {
    if (request.profileName() == null || request.profileName().isBlank()) {
      throw new InvalidRequestException("profileName is required");
    }
    if (profileRegistry.get(request.profileName()) == null) {
      throw new InvalidRequestException("Unknown profile: " + request.profileName());
    }
    String userId = request.userId() != null ? request.userId() : "web";
    Session session = sessionManager.getOrCreate("web", userId, request.profileName());
    return ApiResponse.ok(toResponse(session));
  }

  @PostMapping("/{id}/messages")
  public ApiResponse<MessageResponse> send(
      @PathVariable String id, @RequestBody SendMessageRequest request) {
    if (request.content() == null || request.content().isEmpty()) {
      throw new InvalidRequestException("Message content is required");
    }
    if (request.content().length() > MAX_MESSAGE_SIZE) {
      throw new InvalidRequestException("Message exceeds 32KB limit");
    }
    Session session = sessionManager.get(id);
    if (session == null) {
      throw new SessionNotFoundException(id);
    }
    String reply = agentService.process(session, request.content());
    return ApiResponse.ok(new MessageResponse(reply));
  }

  @GetMapping("/{id}")
  public ApiResponse<SessionResponse> get(@PathVariable String id) {
    Session session = sessionManager.get(id);
    if (session == null) {
      throw new SessionNotFoundException(id);
    }
    return ApiResponse.ok(toResponse(session));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> archive(@PathVariable String id) {
    Session session = sessionManager.get(id);
    if (session == null) {
      throw new SessionNotFoundException(id);
    }
    sessionManager.save(session);
    return ApiResponse.ok();
  }

  @GetMapping
  public ApiResponse<List<SessionResponse>> list() {
    List<SessionResponse> responses =
        sessionManager.listAll().stream().map(this::toResponse).toList();
    return ApiResponse.ok(responses);
  }

  private SessionResponse toResponse(Session session) {
    List<SessionResponse.MessageItem> messages =
        session.messages().stream()
            .skip(Math.max(0, session.messages().size() - MAX_HISTORY_SIZE))
            .map(m -> new SessionResponse.MessageItem(m.role().name(), m.content()))
            .toList();
    return new SessionResponse(session.id(), session.profileName(), messages);
  }
}

package com.ibios.web.controller;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import com.ibios.web.exception.AgentNotFoundException;
import com.ibios.web.exception.InvalidRequestException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.InvokeRequest;
import com.ibios.web.model.InvokeResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentApiController {

  private static final int MAX_MESSAGE_SIZE = 32 * 1024;

  private final AgentService agentService;
  private final SessionManager sessionManager;
  private final ProfileRegistry profileRegistry;

  public AgentApiController(
      AgentService agentService, SessionManager sessionManager, ProfileRegistry profileRegistry) {
    this.agentService = agentService;
    this.sessionManager = sessionManager;
    this.profileRegistry = profileRegistry;
  }

  @PostMapping("/{name}/invoke")
  public ApiResponse<InvokeResponse> invoke(
      @PathVariable String name, @RequestBody InvokeRequest request) {
    if (request.message() == null || request.message().isEmpty()) {
      throw new InvalidRequestException("Message is required");
    }
    if (request.message().length() > MAX_MESSAGE_SIZE) {
      throw new InvalidRequestException("Message exceeds 32KB limit");
    }
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    Session session = sessionManager.getOrCreate("web", "invoke", name);
    String reply = agentService.process(session, request.message());
    return ApiResponse.ok(new InvokeResponse(reply));
  }
}

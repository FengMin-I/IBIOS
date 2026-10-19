package com.ibios.web.controller;

import com.ibios.core.agent.AgentLifecycleService;
import com.ibios.core.agent.AgentView;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Message;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import com.ibios.web.exception.AgentNotFoundException;
import com.ibios.web.exception.InvalidRequestException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.CreateAgentRequest;
import com.ibios.web.model.GenerateRequest;
import com.ibios.web.model.InvokeRequest;
import com.ibios.web.model.InvokeResponse;
import com.ibios.web.model.MessageResponse;
import com.ibios.web.model.SaveFilesRequest;
import com.ibios.web.model.SendMessageRequest;
import com.ibios.web.model.SessionView;
import com.ibios.web.model.UpdateAgentRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentApiController {

  private static final int MAX_MESSAGE_SIZE = 32 * 1024;

  private final AgentLifecycleService lifecycle;
  private final AgentService agentService;
  private final SessionManager sessionManager;
  private final ProfileRegistry profileRegistry;
  private final MemoryService memoryService;

  public AgentApiController(
      AgentLifecycleService lifecycle,
      AgentService agentService,
      SessionManager sessionManager,
      ProfileRegistry profileRegistry,
      MemoryService memoryService) {
    this.lifecycle = lifecycle;
    this.agentService = agentService;
    this.sessionManager = sessionManager;
    this.profileRegistry = profileRegistry;
    this.memoryService = memoryService;
  }

  @PostMapping
  public ApiResponse<AgentView> create(@RequestBody CreateAgentRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new InvalidRequestException("Agent name is required");
    }
    AgentView view = lifecycle.create(request.name(), request.description());
    return ApiResponse.ok(view);
  }

  @GetMapping
  public ApiResponse<List<AgentView>> list() {
    return ApiResponse.ok(lifecycle.list());
  }

  @GetMapping("/{name}")
  public ApiResponse<AgentView> get(@PathVariable String name) {
    AgentView view = lifecycle.get(name);
    if (view == null) {
      throw new AgentNotFoundException(name);
    }
    return ApiResponse.ok(view);
  }

  @PutMapping("/{name}")
  public ApiResponse<AgentView> update(
      @PathVariable String name, @RequestBody UpdateAgentRequest request) {
    if (request.markdown() == null || request.markdown().isBlank()) {
      throw new InvalidRequestException("Markdown content is required");
    }
    AgentView view = lifecycle.update(name, request.markdown());
    return ApiResponse.ok(view);
  }

  @DeleteMapping("/{name}")
  public ApiResponse<Void> delete(@PathVariable String name) {
    lifecycle.delete(name);
    return ApiResponse.ok();
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

  @PostMapping("/{name}/generate-files")
  public ApiResponse<Map<String, String>> generateFiles(
      @PathVariable String name, @RequestBody GenerateRequest request) {
    if (request.description() == null || request.description().isBlank()) {
      throw new InvalidRequestException("Description is required");
    }
    Map<String, String> files = lifecycle.generateFiles(name, request.description());
    return ApiResponse.ok(files);
  }

  @PostMapping("/{name}/files")
  public ApiResponse<AgentView> saveFiles(
      @PathVariable String name, @RequestBody SaveFilesRequest request) {
    if (request.files() == null || request.files().isEmpty()) {
      throw new InvalidRequestException("Files map is required");
    }
    AgentView view = lifecycle.saveFiles(name, request.files());
    return ApiResponse.ok(view);
  }

  @GetMapping("/{name}/memory")
  public ApiResponse<Map<String, String>> getMemory(@PathVariable String name) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    String content = lifecycle.readAgentMemory(name);
    return ApiResponse.ok(Map.of("content", content));
  }

  @PostMapping("/{name}/memory")
  public ApiResponse<Void> saveMemory(
      @PathVariable String name, @RequestBody Map<String, String> request) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    String content = request.get("content");
    lifecycle.saveAgentMemory(name, content);
    return ApiResponse.ok();
  }

  @PostMapping("/{name}/regenerate")
  public ApiResponse<Map<String, String>> regenerate(
      @PathVariable String name, @RequestBody Map<String, String> request) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    String description = request.get("description");
    if (description == null || description.isBlank()) {
      throw new InvalidRequestException("Description is required");
    }
    Map<String, String> files = lifecycle.regenerateAgent(name, description);
    return ApiResponse.ok(files);
  }

  @GetMapping("/{name}/session")
  public ApiResponse<SessionView> getSession(@PathVariable String name) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    Session session = sessionManager.getOrCreate("admin", "console", name);
    List<MessageResponse> messages = new ArrayList<>();
    List<Message> msgs = session.messages();
    int start = Math.max(0, msgs.size() - 100);
    for (int i = start; i < msgs.size(); i++) {
      Message msg = msgs.get(i);
      messages.add(new MessageResponse(msg.content()));
    }
    return ApiResponse.ok(new SessionView(session.id(), session.profileName(), messages));
  }

  @PostMapping("/{name}/session/messages")
  public ApiResponse<InvokeResponse> sendSessionMessage(
      @PathVariable String name, @RequestBody SendMessageRequest request) {
    if (request.content() == null || request.content().isBlank()) {
      throw new InvalidRequestException("Message content is required");
    }
    if (request.content().length() > MAX_MESSAGE_SIZE) {
      throw new InvalidRequestException("Message exceeds 32KB limit");
    }
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new AgentNotFoundException(name);
    }
    Session session = sessionManager.getOrCreate("admin", "console", name);
    String reply = agentService.process(session, request.content());
    return ApiResponse.ok(new InvokeResponse(reply));
  }
}

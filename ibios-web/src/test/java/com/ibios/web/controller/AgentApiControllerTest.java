package com.ibios.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.agent.AgentLifecycleService;
import com.ibios.core.agent.AgentView;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import com.ibios.web.exception.GlobalExceptionHandler;
import com.ibios.web.model.GenerateRequest;
import com.ibios.web.model.InvokeRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgentApiControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  private AgentLifecycleService lifecycle;
  private AgentService agentService;
  private SessionManager sessionManager;
  private ProfileRegistry profileRegistry;
  private MemoryService memoryService;

  @BeforeEach
  void setUp() {
    lifecycle = Mockito.mock(AgentLifecycleService.class);
    agentService = Mockito.mock(AgentService.class);
    sessionManager = Mockito.mock(SessionManager.class);
    profileRegistry = Mockito.mock(ProfileRegistry.class);
    memoryService = Mockito.mock(MemoryService.class);
    objectMapper = new ObjectMapper();

    AgentApiController controller =
        new AgentApiController(
            lifecycle, agentService, sessionManager, profileRegistry, memoryService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private Profile buildProfile(String name) {
    return new Profile(
        name,
        "desc",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20,
        null);
  }

  private AgentView buildAgentView(String name) {
    return new AgentView(
        name,
        "A test agent",
        "dashscope",
        "qwen-plus",
        Collections.emptyList(),
        "/tmp/agents/" + name,
        false,
        false,
        false,
        false,
        Collections.emptyList());
  }

  @Test
  @DisplayName("POST /api/v1/agents returns agent view")
  void create_returnsAgentView() throws Exception {
    AgentView view = buildAgentView("my-agent");
    when(lifecycle.create("my-agent", "A test agent")).thenReturn(view);

    String body =
        objectMapper.writeValueAsString(Map.of("name", "my-agent", "description", "A test agent"));

    mockMvc
        .perform(post("/api/v1/agents").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value("my-agent"))
        .andExpect(jsonPath("$.data.description").value("A test agent"))
        .andExpect(jsonPath("$.data.provider").value("dashscope"))
        .andExpect(jsonPath("$.data.model").value("qwen-plus"));

    verify(lifecycle).create("my-agent", "A test agent");
  }

  @Test
  @DisplayName("POST /api/v1/agents with conflicting name returns 400")
  void create_nameConflict_returns400() throws Exception {
    when(lifecycle.create("dup-agent", "desc"))
        .thenThrow(new IllegalArgumentException("Agent already exists: dup-agent"));

    String body =
        objectMapper.writeValueAsString(Map.of("name", "dup-agent", "description", "desc"));

    mockMvc
        .perform(post("/api/v1/agents").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("Agent already exists: dup-agent"));
  }

  @Test
  @DisplayName("GET /api/v1/agents/{name} when lifecycle throws returns 400")
  void get_notFound_returns400() throws Exception {
    when(lifecycle.get("missing"))
        .thenThrow(new IllegalArgumentException("Agent not found: missing"));

    mockMvc
        .perform(get("/api/v1/agents/missing"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("Agent not found: missing"));
  }

  @Test
  @DisplayName("DELETE /api/v1/agents/{name} calls lifecycle.delete")
  void delete_callsLifecycle() throws Exception {
    mockMvc
        .perform(delete("/api/v1/agents/my-agent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    verify(lifecycle).delete("my-agent");
  }

  @Test
  @DisplayName("POST /api/v1/agents/{name}/invoke calls agentService.process")
  void invoke_callsAgentService() throws Exception {
    Profile profile = buildProfile("test-agent");
    Session session = new Session("s-1", "test-agent", "web", "invoke");

    when(profileRegistry.get("test-agent")).thenReturn(profile);
    when(sessionManager.getOrCreate("web", "invoke", "test-agent")).thenReturn(session);
    when(agentService.process(any(Session.class), eq("hello world"))).thenReturn("Hi back");

    String body = objectMapper.writeValueAsString(new InvokeRequest("hello world"));

    mockMvc
        .perform(
            post("/api/v1/agents/test-agent/invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reply").value("Hi back"));

    verify(agentService).process(any(Session.class), eq("hello world"));
  }

  @Test
  @DisplayName("POST /api/v1/agents/{name}/invoke when agent not found returns 404")
  void invoke_agentNotFound_returns404() throws Exception {
    when(profileRegistry.get("ghost")).thenReturn(null);

    String body = objectMapper.writeValueAsString(new InvokeRequest("hello"));

    mockMvc
        .perform(
            post("/api/v1/agents/ghost/invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("POST /api/v1/agents/{name}/generate-files returns files map")
  void generateFiles_returnsMap() throws Exception {
    Map<String, String> files = Map.of("AGENT.md", "---\nname: gen-agent\n---\n# Gen Agent");
    when(lifecycle.generateFiles("gen-agent", "make a weather agent")).thenReturn(files);

    String body = objectMapper.writeValueAsString(new GenerateRequest("make a weather agent"));

    mockMvc
        .perform(
            post("/api/v1/agents/gen-agent/generate-files")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data['AGENT.md']").exists());

    verify(lifecycle).generateFiles("gen-agent", "make a weather agent");
  }
}

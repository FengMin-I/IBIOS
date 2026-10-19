package com.ibios.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.web.exception.GlobalExceptionHandler;
import com.ibios.web.model.WriteFileRequest;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WorkspaceApiControllerTest {

  @TempDir Path tempDir;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private Path ibiosRoot;

  @BeforeEach
  void setUp() throws Exception {
    ibiosRoot = tempDir.resolve(".ibios");
    Files.createDirectories(ibiosRoot);

    WorkspaceApiController controller = new WorkspaceApiController();
    Field ibiosRootField = WorkspaceApiController.class.getDeclaredField("ibiosRoot");
    ibiosRootField.setAccessible(true);
    ibiosRootField.set(controller, ibiosRoot);

    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/workspace/tree returns root node")
  void tree_returnsAgentsAndArchive() throws Exception {
    Path agentsDir = ibiosRoot.resolve("agents");
    Files.createDirectories(agentsDir);
    Path archiveDir = ibiosRoot.resolve("archive");
    Files.createDirectories(archiveDir);

    mockMvc
        .perform(get("/api/v1/workspace/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value(".ibios"))
        .andExpect(jsonPath("$.data.type").value("directory"))
        .andExpect(jsonPath("$.data.children").isArray());
  }

  @Test
  @DisplayName("GET /api/v1/workspace/file returns file content")
  void readFile_returnsContent() throws Exception {
    Path agentDir = ibiosRoot.resolve("agents/test");
    Files.createDirectories(agentDir);
    Files.writeString(agentDir.resolve("AGENT.md"), "---\nname: test\n---\n# Test Agent");

    mockMvc
        .perform(get("/api/v1/workspace/file").param("path", "agents/test/AGENT.md"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.path").value("agents/test/AGENT.md"))
        .andExpect(jsonPath("$.data.content").value("---\nname: test\n---\n# Test Agent"));
  }

  @Test
  @DisplayName("GET /api/v1/workspace/file with path traversal returns 400")
  void readFile_pathTraversal_returns400() throws Exception {
    mockMvc
        .perform(get("/api/v1/workspace/file").param("path", "../../etc/passwd"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("POST /api/v1/workspace/file creates file")
  void writeFile_createsFile() throws Exception {
    WriteFileRequest request = new WriteFileRequest("agents/new-agent/AGENT.md", "# New Agent");

    mockMvc
        .perform(
            post("/api/v1/workspace/file")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    Path written = ibiosRoot.resolve("agents/new-agent/AGENT.md");
    assert Files.isRegularFile(written);
    assert Files.readString(written).equals("# New Agent");
  }

  @Test
  @DisplayName("POST /api/v1/workspace/file with path traversal returns 400")
  void writeFile_pathTraversal_returns400() throws Exception {
    WriteFileRequest request = new WriteFileRequest("../../etc/passwd", "malicious content");

    mockMvc
        .perform(
            post("/api/v1/workspace/file")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }
}

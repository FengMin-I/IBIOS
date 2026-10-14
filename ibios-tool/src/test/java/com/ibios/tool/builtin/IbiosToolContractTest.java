package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SearchProvider;
import com.ibios.core.tool.UserInteraction;
import com.ibios.tool.interaction.InteractionTools;
import com.ibios.tool.interaction.UnsupportedUserInteraction;
import com.ibios.tool.sandbox.NoopSandbox;
import com.ibios.tool.search.WebSearchTools;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestClient;

class IbiosToolContractTest {

  static Stream<IbiosTool> allRegisteredTools() {
    Sandbox sandbox = new NoopSandbox();
    FileTools fileTools = new FileTools(sandbox);
    ShellTools shellTools = new ShellTools(sandbox);
    HttpTools httpTools = new HttpTools(sandbox, RestClient.builder());

    UserInteraction unsupportedInteraction = new UnsupportedUserInteraction();
    InteractionTools interactionTools = new InteractionTools(unsupportedInteraction);

    SearchProvider emptyProvider = query -> List.of();
    WebSearchTools webSearchTools = new WebSearchTools(emptyProvider, sandbox);

    return Stream.of(
        new FileTools.ReadFileTool(fileTools),
        new FileTools.WriteFileTool(fileTools),
        new FileTools.ListDirTool(fileTools),
        new FileTools.EditFileTool(fileTools),
        new FileTools.GrepTool(fileTools),
        new FileTools.GlobTool(fileTools),
        new ShellTools.ShellTool(shellTools),
        new HttpTools.HttpGetTool(httpTools),
        new HttpTools.HttpPostTool(httpTools),
        new InteractionTools.AskUserTool(interactionTools),
        new WebSearchTools.WebSearchTool(webSearchTools));
  }

  @ParameterizedTest
  @MethodSource("allRegisteredTools")
  void eachToolContractTripleMustBeNonNull(IbiosTool tool) {
    assertNotNull(tool.name(), "Tool name must not be null");
    assertNotNull(tool.description(), "Tool description must not be null");
    assertNotNull(tool.inputSchema(), "Tool inputSchema must not be null");
  }
}

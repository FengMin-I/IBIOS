package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Session;
import com.ibios.core.session.ToolResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReActLoopTest {

  @Mock private ChatPort chatPort;
  @Mock private ToolExecutor toolExecutor;

  private PromptBuilder promptBuilder;
  private ReActLoop reActLoop;

  @BeforeEach
  void setUp() {
    ContextLoader contextLoader = new ContextLoader();
    promptBuilder = new PromptBuilder(contextLoader);
    reActLoop = new ReActLoop(promptBuilder, chatPort, toolExecutor);
  }

  @Test
  void shouldReturnResponseWhenNoToolCalls() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();
    when(chatPort.chat(any(), any(), any()))
        .thenReturn(new ChatResponse("Final answer", Collections.emptyList()));

    String result = reActLoop.run(session, "Hello", profile);

    assertEquals("Final answer", result);
    verify(chatPort, times(1)).chat(any(), any(), any());
  }

  @Test
  void shouldExecuteToolAndContinueLoop() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();

    ChatResponse responseWithTool =
        new ChatResponse("Calling tool", List.of(new ToolCallResponse("test_tool", "{}")));
    ChatResponse finalResponse = new ChatResponse("Final answer", Collections.emptyList());

    when(chatPort.chat(any(), any(), any())).thenReturn(responseWithTool).thenReturn(finalResponse);
    when(toolExecutor.execute(any(), any(), any(), any()))
        .thenReturn(new ToolResult("test_tool", "{}", "tool output", true, null));

    String result = reActLoop.run(session, "Use tool", profile);

    assertEquals("Final answer", result);
    verify(chatPort, times(2)).chat(any(), any(), any());
    verify(toolExecutor, times(1)).execute(eq("s1"), eq("test"), eq("test_tool"), eq("{}"));
  }

  @Test
  void shouldStopAtMaxIterations() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();

    ChatResponse responseWithTool =
        new ChatResponse("Calling tool", List.of(new ToolCallResponse("test_tool", "{}")));
    when(chatPort.chat(any(), any(), any())).thenReturn(responseWithTool);
    when(toolExecutor.execute(any(), any(), any(), any()))
        .thenReturn(new ToolResult("test_tool", "{}", "output", true, null));

    String result = reActLoop.run(session, "Use tool", profile);

    assertTrue(result.contains("达到最大轮数"));
    verify(chatPort, times(10)).chat(any(), any(), any());
  }

  @Test
  void shouldAccumulateMessagesInSession() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile();

    ChatResponse responseWithTool =
        new ChatResponse("Calling tool", List.of(new ToolCallResponse("test_tool", "{}")));
    ChatResponse finalResponse = new ChatResponse("Final answer", Collections.emptyList());

    when(chatPort.chat(any(), any(), any())).thenReturn(responseWithTool).thenReturn(finalResponse);
    when(toolExecutor.execute(any(), any(), any(), any()))
        .thenReturn(new ToolResult("test_tool", "{}", "tool output", true, null));

    reActLoop.run(session, "Hello", profile);

    assertTrue(session.messages().size() >= 4);
  }

  private Profile createProfile() {
    return new Profile(
        "test",
        "test profile",
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
}

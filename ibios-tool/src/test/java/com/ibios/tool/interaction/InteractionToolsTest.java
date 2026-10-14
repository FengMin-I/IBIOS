package com.ibios.tool.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.ToolExecutionResult;
import com.ibios.core.tool.UserInteraction;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InteractionToolsTest {

  @Test
  @DisplayName("ConsoleUserInteraction: 能读终端（mock stdin）")
  void consoleInteractionReadsStdin() {
    String input = "my answer\n";
    InputStream originalIn = System.in;
    try {
      System.setIn(new ByteArrayInputStream(input.getBytes()));
      UserInteraction interaction = new ConsoleUserInteraction();
      String answer = interaction.ask("What is your name?");
      assertEquals("my answer", answer);
    } finally {
      System.setIn(originalIn);
    }
  }

  @Test
  @DisplayName("UnsupportedUserInteraction: 抛异常")
  void unsupportedInteractionThrowsException() {
    UserInteraction interaction = new UnsupportedUserInteraction();
    assertThrows(UnsupportedOperationException.class, () -> interaction.ask("test"));
  }

  @Test
  @DisplayName("AskUserTool: 通过 InteractionTools 调用成功")
  void askUserToolSuccess() {
    String input = "yes\n";
    InputStream originalIn = System.in;
    try {
      System.setIn(new ByteArrayInputStream(input.getBytes()));
      UserInteraction interaction = new ConsoleUserInteraction();
      InteractionTools tools = new InteractionTools(interaction);

      ToolExecutionResult result = tools.doAskUser("{\"question\":\"Continue?\"}");

      assertTrue(result.success());
      assertEquals("yes", result.output());
    } finally {
      System.setIn(originalIn);
    }
  }

  @Test
  @DisplayName("AskUserTool: Unsupported 渠道返回失败")
  void askUserToolUnsupportedChannel() {
    UserInteraction interaction = new UnsupportedUserInteraction();
    InteractionTools tools = new InteractionTools(interaction);

    ToolExecutionResult result = tools.doAskUser("{\"question\":\"Continue?\"}");

    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("not supported"));
  }
}

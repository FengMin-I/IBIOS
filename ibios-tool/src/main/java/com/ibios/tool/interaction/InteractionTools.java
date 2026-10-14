package com.ibios.tool.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.ToolExecutionResult;
import com.ibios.core.tool.UserInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InteractionTools {

  private static final Logger log = LoggerFactory.getLogger(InteractionTools.class);

  private final UserInteraction userInteraction;
  private final ObjectMapper objectMapper;

  public InteractionTools(UserInteraction userInteraction) {
    this.userInteraction = userInteraction;
    this.objectMapper = new ObjectMapper();
  }

  @Component("askUserTool")
  public static final class AskUserTool implements IbiosTool {

    private final InteractionTools delegate;

    public AskUserTool(InteractionTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "ask_user";
    }

    @Override
    public String description() {
      return "Ask the user a question and wait for their answer (human-in-the-loop)";
    }

    @Override
    public String inputSchema() {
      return """
          {
            "type": "object",
            "properties": {
              "question": {"type": "string", "description": "The question to ask the user"}
            },
            "required": ["question"]
          }
          """;
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doAskUser(argumentsJson);
    }
  }

  ToolExecutionResult doAskUser(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String question = args.get("question").asText();
      log.info("ask_user: {}", question);
      String answer = userInteraction.ask(question);
      return ToolExecutionResult.success(answer);
    } catch (UnsupportedOperationException e) {
      return ToolExecutionResult.failure(e.getMessage(), false);
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to ask user: " + e.getMessage(), false);
    }
  }
}

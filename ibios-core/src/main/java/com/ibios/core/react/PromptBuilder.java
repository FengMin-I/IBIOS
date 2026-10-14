package com.ibios.core.react;

import com.ibios.core.port.ChatPort.Prompt;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.core.session.Session;
import com.ibios.core.tool.IbiosTool;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PromptBuilder {
  private final ContextLoader contextLoader;

  public PromptBuilder(ContextLoader contextLoader) {
    this.contextLoader = contextLoader;
  }

  public Prompt build(Session session, Profile profile) {
    String systemPrompt = buildSystemPrompt(profile);
    List<Message> messages = truncateHistory(session.messages(), profile.maxHistoryTurns());
    List<IbiosTool> tools = Collections.emptyList();
    return new Prompt(systemPrompt, messages, tools);
  }

  private String buildSystemPrompt(Profile profile) {
    StringBuilder sb = new StringBuilder();

    String identityPrompt = profile.identity().prompt();
    if (identityPrompt != null && !identityPrompt.isEmpty()) {
      sb.append(identityPrompt).append("\n\n");
    }

    String bootstrapContent = contextLoader.loadBootstrapContent(profile);
    if (!bootstrapContent.isEmpty()) {
      sb.append(bootstrapContent).append("\n\n");
    }

    String skillContent = contextLoader.loadSkillContent(profile);
    if (!skillContent.isEmpty()) {
      sb.append(skillContent).append("\n\n");
    }

    String currentDateTime = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    sb.append("Current date and time: ").append(currentDateTime);

    return sb.toString();
  }

  private List<Message> truncateHistory(List<Message> messages, int maxHistoryTurns) {
    int maxMessages = maxHistoryTurns * 2;
    if (messages.size() <= maxMessages) {
      return new ArrayList<>(messages);
    }
    int startIndex = messages.size() - maxMessages;
    return new ArrayList<>(messages.subList(startIndex, messages.size()));
  }
}

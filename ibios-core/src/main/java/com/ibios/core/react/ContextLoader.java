package com.ibios.core.react;

import com.ibios.core.profile.Profile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContextLoader {
  private static final Logger LOG = Logger.getLogger(ContextLoader.class.getName());
  private static final Pattern FRONTMATTER_PATTERN =
      Pattern.compile("\\A---\\s*\\n.*?\\n---\\s*\\n(.*)\\z", Pattern.DOTALL);

  public String loadBootstrapContent(Profile profile) {
    List<String> bootstrapPaths = profile.bootstrap();
    if (bootstrapPaths == null || bootstrapPaths.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (String pathStr : bootstrapPaths) {
      Path path = Path.of(pathStr);
      if (!Files.exists(path)) {
        LOG.log(Level.WARNING, "Bootstrap file not found: {0}", pathStr);
        continue;
      }
      try {
        String content = Files.readString(path);
        sb.append(content).append("\n");
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed to read bootstrap file: " + pathStr, e);
      }
    }
    return sb.toString();
  }

  public String loadSkillContent(Profile profile) {
    List<String> skillPaths = profile.skills();
    if (skillPaths == null || skillPaths.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (String pathStr : skillPaths) {
      Path path = Path.of(pathStr);
      if (!Files.exists(path)) {
        throw new IllegalStateException("Skill file not found: " + pathStr);
      }
      try {
        String content = Files.readString(path);
        sb.append(content).append("\n");
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read skill file: " + pathStr, e);
      }
    }
    return sb.toString();
  }

  public String loadAgentBody(Profile profile) {
    Path agentDir = profile.agentDir();
    if (agentDir == null) {
      return "";
    }
    Path agentMd = agentDir.resolve("AGENT.md");
    if (!Files.isRegularFile(agentMd)) {
      LOG.log(Level.WARNING, "AGENT.md not found: {0}", agentMd);
      return "";
    }
    try {
      String raw = Files.readString(agentMd);
      Matcher matcher = FRONTMATTER_PATTERN.matcher(raw);
      if (matcher.matches()) {
        return matcher.group(1).trim();
      }
      return raw;
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to read AGENT.md: " + agentMd, e);
      return "";
    }
  }
}

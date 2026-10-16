package com.ibios.core.agent;

import java.nio.file.Path;
import java.util.Map;

public record AgentDefinition(
    String name,
    Map<String, Object> frontmatter,
    String body,
    Path agentDir,
    Path scriptsDir,
    Path skillsDir,
    Path referenceFile) {}

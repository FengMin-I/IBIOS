package com.ibios.tool.sandbox;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public record FileSandboxProperties(List<String> allowedPaths) {}

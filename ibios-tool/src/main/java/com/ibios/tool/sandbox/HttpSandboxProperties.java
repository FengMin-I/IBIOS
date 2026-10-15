package com.ibios.tool.sandbox;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "http")
public record HttpSandboxProperties(List<String> allowedDomains) {}

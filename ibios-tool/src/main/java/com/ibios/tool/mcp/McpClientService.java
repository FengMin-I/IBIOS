package com.ibios.tool.mcp;

import com.ibios.core.port.ToolRegistry;
import com.ibios.core.workspace.WorkspaceRoot;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Connects to configured MCP servers on startup and registers their tools in the ToolRegistry.
 *
 * <p>TODO: Complete MCP SDK integration with actual client connections
 */
@Component
public class McpClientService {

  private static final Logger log = LoggerFactory.getLogger(McpClientService.class);
  private static final String DEFAULT_CONFIG_LOCATION =
      WorkspaceRoot.DEFAULT_ROOT + "/mcp_servers.yaml";
  private static final String CLASSPATH_PREFIX = "classpath:";
  private static final String FILE_PREFIX = "file:";

  private final ToolRegistry toolRegistry;

  @Value("${ibios.mcp.config-location:${ibios.root:.ibios}/mcp_servers.yaml}")
  private String configLocation = DEFAULT_CONFIG_LOCATION;

  public McpClientService(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void connectAll() {
    List<Map<String, Object>> servers = loadConfigs();
    if (servers.isEmpty()) {
      log.info("No MCP servers configured");
      return;
    }

    for (Map<String, Object> serverConfig : servers) {
      String serverName = (String) serverConfig.get("name");
      try {
        connectServer(serverName, serverConfig);
      } catch (Exception e) {
        log.warn("MCP server {} connection failed, skipping its tools", serverName, e);
        // External dependency failure doesn't affect our startup
      }
    }
  }

  private void connectServer(String serverName, Map<String, Object> config) {
    // TODO: Implement actual MCP client connection using the SDK
    // For now, log that we would connect
    log.info(
        "Would connect to MCP server: {} (transport: {})", serverName, config.get("transport"));

    // Placeholder: In a real implementation, we would:
    // 1. Create MCP client based on transport (stdio/sse)
    // 2. Call tools/list to get available tools
    // 3. Wrap each tool in McpToolAdapter
    // 4. Register in toolRegistry
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> loadConfigs() {
    String location = resolveLocation();
    try (InputStream inputStream = openConfig(location)) {
      if (inputStream == null) {
        log.debug("MCP config not found: {}", location);
        return Collections.emptyList();
      }
      Map<String, Object> config = new Yaml().load(inputStream);
      if (config == null || !config.containsKey("servers")) {
        return Collections.emptyList();
      }
      return (List<Map<String, Object>>) config.get("servers");
    } catch (Exception e) {
      log.warn("Failed to load MCP server configs from {}", location, e);
      return Collections.emptyList();
    }
  }

  private String resolveLocation() {
    return configLocation == null || configLocation.isBlank()
        ? DEFAULT_CONFIG_LOCATION
        : configLocation.trim();
  }

  private InputStream openConfig(String location) throws IOException {
    if (location.startsWith(CLASSPATH_PREFIX)) {
      String resource = location.substring(CLASSPATH_PREFIX.length());
      return getClass().getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
    }
    String path =
        location.startsWith(FILE_PREFIX) ? location.substring(FILE_PREFIX.length()) : location;
    Path configFile = Path.of(path);
    return Files.isRegularFile(configFile) ? Files.newInputStream(configFile) : null;
  }
}

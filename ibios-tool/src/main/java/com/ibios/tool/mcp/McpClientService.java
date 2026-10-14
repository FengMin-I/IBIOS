package com.ibios.tool.mcp;

import com.ibios.core.port.ToolRegistry;
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

  private final ToolRegistry toolRegistry;

  @Value("${ibios.mcp.config-location:classpath:mcp_servers.yaml}")
  private String configLocation;

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
    try {
      Yaml yaml = new Yaml();
      var inputStream = getClass().getResourceAsStream("/mcp_servers.yaml");
      if (inputStream == null) {
        log.debug("No mcp_servers.yaml found");
        return Collections.emptyList();
      }
      Map<String, Object> config = yaml.load(inputStream);
      if (config == null || !config.containsKey("servers")) {
        return Collections.emptyList();
      }
      return (List<Map<String, Object>>) config.get("servers");
    } catch (Exception e) {
      log.warn("Failed to load MCP server configs", e);
      return Collections.emptyList();
    }
  }
}

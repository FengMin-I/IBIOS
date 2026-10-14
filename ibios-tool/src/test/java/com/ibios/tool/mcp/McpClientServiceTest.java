package com.ibios.tool.mcp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.ibios.core.port.ToolRegistry;
import org.junit.jupiter.api.Test;

class McpClientServiceTest {

  @Test
  void connectAllShouldNotThrowWhenNoServersConfigured() {
    ToolRegistry registry = mock(ToolRegistry.class);
    McpClientService service = new McpClientService(registry);

    // Should not throw even with no config
    service.connectAll();
    assertNotNull(service);
  }
}

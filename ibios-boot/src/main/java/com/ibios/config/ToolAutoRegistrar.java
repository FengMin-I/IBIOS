package com.ibios.config;

import com.ibios.core.port.ToolRegistry;
import com.ibios.core.tool.IbiosTool;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolAutoRegistrar {

  private static final Logger log = LoggerFactory.getLogger(ToolAutoRegistrar.class);

  private final ToolRegistry toolRegistry;
  private final ApplicationContext applicationContext;

  public ToolAutoRegistrar(ToolRegistry toolRegistry, ApplicationContext applicationContext) {
    this.toolRegistry = toolRegistry;
    this.applicationContext = applicationContext;
  }

  @PostConstruct
  public void registerAllTools() {
    log.info(" 自动注册 Spring 容器中的工具 Bean...");

    Map<String, IbiosTool> toolBeans = applicationContext.getBeansOfType(IbiosTool.class);

    for (Map.Entry<String, IbiosTool> entry : toolBeans.entrySet()) {
      IbiosTool tool = entry.getValue();
      toolRegistry.register(tool);
      log.debug("   - {} ({})", tool.name(), entry.getKey());
    }

    log.info("✅ 已注册 {} 个工具", toolRegistry.listAll().size());
  }
}

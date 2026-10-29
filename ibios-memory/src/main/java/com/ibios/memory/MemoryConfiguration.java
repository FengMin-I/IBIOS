package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.workspace.WorkspaceRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MemoryConfiguration {

  @Bean
  @ConditionalOnProperty(name = "memory.backend", havingValue = "markdown", matchIfMissing = true)
  public LongTermMemoryStore markdownMemoryStore(@Value("${ibios.root:.ibios}") String ibiosRoot) {
    return new MarkdownMemoryStore(new WorkspaceRoot(ibiosRoot).memoryFile());
  }

  @Bean
  @ConditionalOnProperty(name = "memory.backend", havingValue = "sqlite")
  public LongTermMemoryStore sqliteMemoryStore(MemoryEntryRepository repository) {
    return new SqliteMemoryStore(repository);
  }

  @Bean
  @ConditionalOnProperty(name = "memory.backend", havingValue = "mem0")
  public LongTermMemoryStore mem0MemoryStore(
      RestClient.Builder restClientBuilder, org.springframework.core.env.Environment env) {
    String baseUrl = env.getProperty("memory.mem0.base-url", "http://localhost:8080");
    RestClient restClient = restClientBuilder.build();
    return new Mem0MemoryStore(restClient, baseUrl);
  }

  @Bean
  public MemoryService memoryService(
      LongTermMemoryStore store, @Value("${ibios.root:.ibios}") String ibiosRoot) {
    return new MemoryServiceImpl(store, new WorkspaceRoot(ibiosRoot).agentsDir());
  }

  @Bean
  public MemoryTools memoryTools(MemoryService memoryService) {
    return new MemoryTools(memoryService);
  }

  @Bean
  public IbiosTool saveMemoryTool(MemoryTools memoryTools) {
    return memoryTools.saveMemoryTool();
  }

  @Bean
  public IbiosTool recallMemoryTool(MemoryTools memoryTools) {
    return memoryTools.recallMemoryTool();
  }
}

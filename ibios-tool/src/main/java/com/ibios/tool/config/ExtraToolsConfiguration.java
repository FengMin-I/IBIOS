package com.ibios.tool.config;

import com.ibios.core.tool.SearchProvider;
import com.ibios.core.tool.UserInteraction;
import com.ibios.tool.interaction.ConsoleUserInteraction;
import com.ibios.tool.search.DuckDuckGoSearchProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 扩展工具的配置：UserInteraction 和 SearchProvider 的默认实现。 */
@Configuration
public class ExtraToolsConfiguration {

  @Bean
  public UserInteraction userInteraction() {
    return new ConsoleUserInteraction();
  }

  @Bean
  public SearchProvider searchProvider(RestClient.Builder restClientBuilder) {
    return new DuckDuckGoSearchProvider(restClientBuilder);
  }
}

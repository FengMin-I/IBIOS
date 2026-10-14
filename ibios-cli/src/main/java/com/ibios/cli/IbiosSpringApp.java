package com.ibios.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {
      "com.ibios.core",
      "com.ibios.storage",
      "com.ibios.provider",
      "com.ibios.channel.cli"
    })
@EnableJpaRepositories(basePackages = "com.ibios.storage")
@EntityScan(basePackages = "com.ibios.storage")
public class IbiosSpringApp {

  public static ConfigurableApplicationContext start(String[] args) {
    return SpringApplication.run(IbiosSpringApp.class, args);
  }
}

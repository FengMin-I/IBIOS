package com.ibios;

import com.ibios.cli.IbiosOsCli;
import java.util.Set;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IbiosApplication {

  private static final Set<String> HEAVY_COMMANDS = Set.of("chat", "serve", "gateway");

  public static void main(String[] args) {
    if (isLightCommand(args)) {
      IbiosOsCli.main(args);
      return;
    }
    SpringApplication.run(IbiosApplication.class, args);
  }

  private static boolean isLightCommand(String[] args) {
    if (args.length == 0) {
      return true;
    }
    String first = args[0];
    if (first.startsWith("-")) {
      return true;
    }
    return !HEAVY_COMMANDS.contains(first);
  }
}

package com.ibios.tool.interaction;

import com.ibios.core.tool.UserInteraction;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/** CLI 渠道的交互实现：从标准输入读取用户回答。 */
public class ConsoleUserInteraction implements UserInteraction {

  @Override
  public String ask(String question) {
    System.out.println(question);
    System.out.print("> ");
    System.out.flush();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line = reader.readLine();
      return line != null ? line.trim() : "";
    } catch (Exception e) {
      throw new RuntimeException("Failed to read user input from console", e);
    }
  }
}

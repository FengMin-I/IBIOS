package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "list", description = "列出已配置的 LLM Provider")
public class ProviderListCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("已配置的 Provider:");
    System.out.println("  dashscope (阿里云灵积)");
    System.out.println("  openai (OpenAI)");
    System.out.println("提示: Provider 通过环境变量配置 API Key");
  }
}

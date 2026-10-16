package com.ibios.tool.sandbox;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxViolationException;
import java.net.URI;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class WhitelistSandbox implements Sandbox {

  private final WhitelistManager whitelistManager;

  public WhitelistSandbox(WhitelistManager whitelistManager) {
    this.whitelistManager = whitelistManager;
  }

  @Override
  public void enforce(SandboxAction action) {
    switch (action.type()) {
      case FILE_READ:
      case FILE_WRITE:
        checkFilePath(action.target());
        break;
      case SHELL_COMMAND:
        checkShellCommand(action.target());
        break;
      case HTTP_REQUEST:
        checkHttpUrl(action.target());
        break;
      default:
        break;
    }
  }

  private void checkFilePath(String rawPath) {
    Path target = Path.of(rawPath).normalize().toAbsolutePath();
    boolean allowed =
        whitelistManager.getAllowedRoots().stream()
            .anyMatch(root -> target.startsWith(root.toAbsolutePath().normalize()));
    if (!allowed) {
      throw new SandboxViolationException("路径不在白名单内: " + rawPath);
    }
  }

  private void checkShellCommand(String command) {
    String firstToken = command.trim().split("\\s+")[0];
    if (!whitelistManager.getAllowedCommands().contains(firstToken)) {
      throw new SandboxViolationException("命令不在白名单内: " + firstToken);
    }
  }

  private void checkHttpUrl(String url) {
    String host = URI.create(url).getHost();
    boolean allowed =
        whitelistManager.getAllowedDomainPatterns().stream()
            .anyMatch(pattern -> matchesDomain(host, pattern));
    if (!allowed) {
      throw new SandboxViolationException("域名不在白名单内: " + host);
    }
  }

  private boolean matchesDomain(String host, String pattern) {
    if ("*".equals(pattern)) {
      return true;
    }
    if (pattern.startsWith("*.")) {
      return host.endsWith(pattern.substring(1)) && (host.length() > pattern.length() - 1);
    }
    return host.equals(pattern);
  }
}

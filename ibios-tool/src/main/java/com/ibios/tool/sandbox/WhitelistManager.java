package com.ibios.tool.sandbox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;

@Component
public class WhitelistManager {

  private final CopyOnWriteArrayList<Path> allowedRoots;
  private final CopyOnWriteArraySet<String> allowedCommands;
  private final CopyOnWriteArrayList<String> allowedDomainPatterns;

  public WhitelistManager(
      FileSandboxProperties fileProps,
      ShellSandboxProperties shellProps,
      HttpSandboxProperties httpProps) {
    this.allowedRoots =
        new CopyOnWriteArrayList<>(
            fileProps.allowedPaths().stream().map(Path::of).map(Path::normalize).toList());
    this.allowedCommands = new CopyOnWriteArraySet<>(shellProps.allowedCommands());
    this.allowedDomainPatterns = new CopyOnWriteArrayList<>(httpProps.allowedDomains());
  }

  public List<Path> getAllowedRoots() {
    return Collections.unmodifiableList(allowedRoots);
  }

  public Set<String> getAllowedCommands() {
    return Collections.unmodifiableSet(allowedCommands);
  }

  public List<String> getAllowedDomainPatterns() {
    return Collections.unmodifiableList(allowedDomainPatterns);
  }

  public void addFilePath(String path) {
    Path normalized = Path.of(path).normalize();
    if (!allowedRoots.contains(normalized)) {
      allowedRoots.add(normalized);
    }
  }

  public boolean removeFilePath(String path) {
    return allowedRoots.removeIf(p -> p.equals(Path.of(path).normalize()));
  }

  public void addShellCommand(String command) {
    allowedCommands.add(command);
  }

  public boolean removeShellCommand(String command) {
    return allowedCommands.remove(command);
  }

  public void addHttpDomain(String domain) {
    if (!allowedDomainPatterns.contains(domain)) {
      allowedDomainPatterns.add(domain);
    }
  }

  public boolean removeHttpDomain(String domain) {
    return allowedDomainPatterns.removeIf(d -> d.equals(domain));
  }

  public WhitelistSnapshot snapshot() {
    return new WhitelistSnapshot(
        allowedRoots.stream().map(Path::toString).toList(),
        new ArrayList<>(allowedCommands),
        new ArrayList<>(allowedDomainPatterns));
  }

  public record WhitelistSnapshot(
      List<String> filePaths, List<String> shellCommands, List<String> httpDomains) {}
}

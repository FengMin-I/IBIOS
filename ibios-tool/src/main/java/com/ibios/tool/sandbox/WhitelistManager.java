package com.ibios.tool.sandbox;

import com.ibios.core.sandbox.SandboxWhitelistStore;
import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import com.ibios.core.workspace.WorkspaceRoot;
import jakarta.annotation.PostConstruct;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 沙箱白名单：DB 为唯一事实源，内存只做执行期快照。启动时把配置 insert-if-absent 播种进库再全量载入； 运行时增删先写库、再刷新快照，沙箱下一次工具调用即生效。 */
@Component
public class WhitelistManager {

  public enum DeleteOutcome {
    DELETED,
    NOT_FOUND,
    PROTECTED
  }

  private final FileSandboxProperties fileProps;
  private final ShellSandboxProperties shellProps;
  private final HttpSandboxProperties httpProps;
  private final SandboxWhitelistStore store;
  private final Set<String> protectedFilePaths;

  private final CopyOnWriteArrayList<Path> allowedRoots = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArraySet<String> allowedCommands = new CopyOnWriteArraySet<>();
  private final CopyOnWriteArrayList<String> allowedDomainPatterns = new CopyOnWriteArrayList<>();

  @Autowired
  public WhitelistManager(
      FileSandboxProperties fileProps,
      ShellSandboxProperties shellProps,
      HttpSandboxProperties httpProps,
      SandboxWhitelistStore store,
      @Value("${ibios.root:.ibios}") String ibiosRoot) {
    this(fileProps, shellProps, httpProps, store, List.of(ibiosRoot));
  }

  public WhitelistManager(
      FileSandboxProperties fileProps,
      ShellSandboxProperties shellProps,
      HttpSandboxProperties httpProps,
      SandboxWhitelistStore store) {
    this(fileProps, shellProps, httpProps, store, List.of(WorkspaceRoot.DEFAULT_ROOT));
  }

  private WhitelistManager(
      FileSandboxProperties fileProps,
      ShellSandboxProperties shellProps,
      HttpSandboxProperties httpProps,
      SandboxWhitelistStore store,
      List<String> workspaceRoots) {
    this.fileProps = fileProps;
    this.shellProps = shellProps;
    this.httpProps = httpProps;
    this.store = store;
    Set<String> roots = new LinkedHashSet<>();
    workspaceRoots.forEach(root -> roots.add(normalizeFileValue(root)));
    this.protectedFilePaths = Collections.unmodifiableSet(roots);
  }

  @PostConstruct
  public void initialize() {
    seedFromConfig();
    reloadFromStore();
  }

  public List<WhitelistEntryView> entries() {
    return store.listAll();
  }

  public List<Path> getAllowedRoots() {
    return Collections.unmodifiableList(new ArrayList<>(allowedRoots));
  }

  public Set<String> getAllowedCommands() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(allowedCommands));
  }

  public List<String> getAllowedDomainPatterns() {
    return Collections.unmodifiableList(new ArrayList<>(allowedDomainPatterns));
  }

  /** 已存在返回 false，不写库；重复新增由调用方决定如何提示。 */
  public boolean addEntry(WhitelistType type, String value) {
    String normalized = normalize(type, value);
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Whitelist value is required");
    }
    if (store.exists(type, normalized)) {
      return false;
    }
    store.create(type, normalized);
    reloadFromStore();
    return true;
  }

  public DeleteOutcome removeById(Long id) {
    WhitelistEntryView entry = store.findById(id);
    if (entry == null) {
      return DeleteOutcome.NOT_FOUND;
    }
    if (entry.type() == WhitelistType.FILE
        && protectedFilePaths.contains(normalizeFileValue(entry.value()))) {
      return DeleteOutcome.PROTECTED;
    }
    store.delete(id);
    reloadFromStore();
    return DeleteOutcome.DELETED;
  }

  public boolean removeEntry(WhitelistType type, String value) {
    String normalized = normalize(type, value);
    for (WhitelistEntryView entry : store.listAll()) {
      if (entry.type() == type && normalize(type, entry.value()).equals(normalized)) {
        return removeById(entry.id()) == DeleteOutcome.DELETED;
      }
    }
    return false;
  }

  public boolean addFilePath(String path) {
    return addEntry(WhitelistType.FILE, path);
  }

  public boolean removeFilePath(String path) {
    return removeEntry(WhitelistType.FILE, path);
  }

  public boolean addShellCommand(String command) {
    return addEntry(WhitelistType.SHELL, command);
  }

  public boolean removeShellCommand(String command) {
    return removeEntry(WhitelistType.SHELL, command);
  }

  public boolean addHttpDomain(String domain) {
    return addEntry(WhitelistType.HTTP, domain);
  }

  public boolean removeHttpDomain(String domain) {
    return removeEntry(WhitelistType.HTTP, domain);
  }

  public WhitelistSnapshot snapshot() {
    return new WhitelistSnapshot(
        allowedRoots.stream().map(Path::toString).toList(),
        new ArrayList<>(allowedCommands),
        new ArrayList<>(allowedDomainPatterns));
  }

  private void seedFromConfig() {
    Set<String> filePaths = new LinkedHashSet<>();
    fileProps.allowedPaths().forEach(path -> filePaths.add(normalizeFileValue(path)));
    // 工作区根必须始终可读写，即使配置里被删掉也要补回
    filePaths.addAll(protectedFilePaths);
    filePaths.forEach(path -> ensureExists(WhitelistType.FILE, path));
    shellProps.allowedCommands().forEach(command -> ensureExists(WhitelistType.SHELL, command));
    httpProps.allowedDomains().forEach(domain -> ensureExists(WhitelistType.HTTP, domain));
  }

  private void ensureExists(WhitelistType type, String value) {
    String normalized = normalize(type, value);
    if (normalized.isEmpty() || store.exists(type, normalized)) {
      return;
    }
    store.create(type, normalized);
  }

  private void reloadFromStore() {
    List<Path> roots = new ArrayList<>();
    List<String> commands = new ArrayList<>();
    List<String> domains = new ArrayList<>();
    for (WhitelistEntryView entry : store.listAll()) {
      switch (entry.type()) {
        case FILE:
          roots.add(Path.of(entry.value()).normalize());
          break;
        case SHELL:
          commands.add(entry.value());
          break;
        case HTTP:
          domains.add(entry.value());
          break;
        default:
          break;
      }
    }
    allowedRoots.clear();
    allowedRoots.addAll(roots);
    allowedCommands.clear();
    allowedCommands.addAll(commands);
    allowedDomainPatterns.clear();
    allowedDomainPatterns.addAll(domains);
  }

  private static String normalize(WhitelistType type, String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    return type == WhitelistType.FILE ? normalizeFileValue(trimmed) : trimmed;
  }

  private static String normalizeFileValue(String value) {
    try {
      return Path.of(value).normalize().toString();
    } catch (InvalidPathException e) {
      throw new IllegalArgumentException("Invalid file path: " + value);
    }
  }

  public record WhitelistSnapshot(
      List<String> filePaths, List<String> shellCommands, List<String> httpDomains) {}
}

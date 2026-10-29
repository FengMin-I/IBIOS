package com.ibios.web.controller;

import com.ibios.core.workspace.WorkspaceRoot;
import com.ibios.web.exception.InvalidRequestException;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.FileContentResponse;
import com.ibios.web.model.FileNode;
import com.ibios.web.model.WriteFileRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceApiController {

  private final Path ibiosRoot;

  public WorkspaceApiController() {
    this(WorkspaceRoot.DEFAULT_ROOT);
  }

  @Autowired
  public WorkspaceApiController(@Value("${ibios.root:.ibios}") String root) {
    this.ibiosRoot = Path.of(root).toAbsolutePath().normalize();
  }

  @GetMapping("/tree")
  public ApiResponse<FileNode> tree(@RequestParam(required = false) String path) {
    boolean rootRequest = path == null || path.isEmpty();
    Path targetDir = rootRequest ? ibiosRoot : resolveAndValidate(path);
    String rootPath = rootRequest ? rootName() : path;
    FileNode root = buildTree(targetDir, rootPath, 0);
    return ApiResponse.ok(root);
  }

  private String rootName() {
    Path name = ibiosRoot.getFileName();
    return name == null ? ibiosRoot.toString() : name.toString();
  }

  @GetMapping("/file")
  public ApiResponse<FileContentResponse> readFile(@RequestParam String path) {
    Path resolved = resolveAndValidate(path);
    if (!Files.isRegularFile(resolved)) {
      throw new InvalidRequestException("File not found: " + path);
    }
    try {
      String content = Files.readString(resolved);
      return ApiResponse.ok(new FileContentResponse(path, content));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + path, e);
    }
  }

  @PostMapping("/file")
  public ApiResponse<Void> writeFile(@RequestBody WriteFileRequest request) {
    Path resolved = resolveAndValidate(request.path());
    try {
      Files.createDirectories(resolved.getParent());
      Files.writeString(resolved, request.content());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file: " + request.path(), e);
    }
    return ApiResponse.ok();
  }

  private Path resolveAndValidate(String path) {
    Path resolved = ibiosRoot.resolve(path).normalize();
    if (!resolved.startsWith(ibiosRoot)) {
      throw new InvalidRequestException("Path traversal detected: " + path);
    }
    return resolved;
  }

  private FileNode buildTree(Path dir, String relativePath, int depth) {
    if (!Files.isDirectory(dir)) {
      return new FileNode(dir.getFileName().toString(), relativePath, "file", List.of());
    }
    List<FileNode> children = new ArrayList<>();
    try (Stream<Path> entries = Files.list(dir)) {
      List<Path> sorted =
          entries.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
      for (Path entry : sorted) {
        String name = entry.getFileName().toString();
        if (name.startsWith(".")) {
          continue;
        }
        String childPath = relativePath + "/" + name;
        if (Files.isDirectory(entry)) {
          children.add(buildTree(entry, childPath, depth + 1));
        } else {
          children.add(new FileNode(name, childPath, "file", List.of()));
        }
      }
    } catch (IOException e) {
      // skip unreadable directories
    }
    return new FileNode(dir.getFileName().toString(), relativePath, "directory", children);
  }
}

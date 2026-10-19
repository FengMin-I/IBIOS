package com.ibios.web.model;

import java.util.List;

public record FileNode(String name, String path, String type, List<FileNode> children) {}

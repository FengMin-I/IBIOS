package com.ibios.core.session;

public record ToolResult(
    String toolName, String inputJson, String output, boolean success, String errorMessage) {}

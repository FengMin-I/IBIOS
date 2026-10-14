package com.ibios.core.tool;

public record SandboxAction(ActionType type, String target) {

  public enum ActionType {
    FILE_READ,
    FILE_WRITE,
    SHELL_COMMAND,
    HTTP_REQUEST
  }
}

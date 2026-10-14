package com.ibios.tool.sandbox;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import org.springframework.stereotype.Component;

/**
 * No-op sandbox implementation that allows all actions. Placeholder until WhitelistSandbox is
 * delivered in Lesson 23/24.
 */
@Component
public class NoopSandbox implements Sandbox {

  @Override
  public void enforce(SandboxAction action) {
    // No-op: allow everything. WhitelistSandbox will replace this in Lesson 23/24.
  }
}

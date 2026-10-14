package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.audit.ToolInvocationAudit;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.port.ToolRegistry.IbiosToolLookup;
import com.ibios.core.port.ToolRegistry.ToolExecutionResult;
import com.ibios.core.session.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

  @Mock private ToolRegistry toolRegistry;
  @Mock private ToolInvocationAudit audit;

  private ToolExecutor toolExecutor;

  @BeforeEach
  void setUp() {
    toolExecutor = new ToolExecutor(toolRegistry, audit);
  }

  @Test
  void shouldRecordSuccessAudit() {
    IbiosToolLookup tool = mock(IbiosToolLookup.class);
    when(toolRegistry.lookup("test_tool")).thenReturn(tool);
    when(tool.execute("{\"arg\":\"value\"}"))
        .thenReturn(new ToolExecutionResult("output", true, null));

    ToolResult result = toolExecutor.execute("s1", "test_tool", "{\"arg\":\"value\"}");

    assertEquals("output", result.output());
    assertTrue(result.success());
    verify(audit)
        .recordToolInvocation(
            eq("s1"), eq("test_tool"), anyString(), anyString(), eq(true), any(), anyLong());
  }

  @Test
  void shouldRecordFailureAuditWithErrorMessage() {
    IbiosToolLookup tool = mock(IbiosToolLookup.class);
    when(toolRegistry.lookup("test_tool")).thenReturn(tool);
    when(tool.execute("{\"arg\":\"value\"}")).thenThrow(new RuntimeException("Tool failed"));

    ToolResult result = toolExecutor.execute("s1", "test_tool", "{\"arg\":\"value\"}");

    assertFalse(result.success());
    assertEquals("Tool failed", result.errorMessage());
    verify(audit)
        .recordToolInvocation(
            eq("s1"), eq("test_tool"), anyString(), any(), eq(false), eq("Tool failed"), anyLong());
  }
}

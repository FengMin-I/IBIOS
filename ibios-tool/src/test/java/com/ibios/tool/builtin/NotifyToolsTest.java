package com.ibios.tool.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.memory.MemoryScope;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.notify.NotifyChannelAdapter;
import com.ibios.core.notify.NotifyChannelStore;
import com.ibios.core.notify.NotifyChannelView;
import com.ibios.core.notify.NotifyTarget;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.ToolExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("NotifyTools 测试")
class NotifyToolsTest {

  private NotifyChannelAdapter adapter;
  private Sandbox sandbox;
  private MemoryService memoryService;
  private NotifyChannelStore channelStore;
  private NotifyTools notifyTools;

  @BeforeEach
  void setUp() {
    adapter = mock(NotifyChannelAdapter.class);
    sandbox = mock(Sandbox.class);
    memoryService = mock(MemoryService.class);
    channelStore = mock(NotifyChannelStore.class);
    notifyTools = new NotifyTools(adapter, sandbox, memoryService, channelStore);
  }

  @Test
  @DisplayName("从全局注册表解析渠道，发送后自动保存到记忆")
  void execute_resolvesGlobalChannelAndSavesMemory() {
    when(channelStore.getByName("lark"))
        .thenReturn(
            new NotifyChannelView(1L, "lark", "webhook", "https://lark.example.com/webhook", ""));

    ToolExecutionResult result =
        notifyTools.execute("{\"channel\":\"lark\",\"content\":\"测试消息：今日天气晴朗\"}");

    assertThat(result.success()).isTrue();
    assertThat(result.output()).contains("lark");

    verify(adapter).send(any(NotifyTarget.class), eq("测试消息：今日天气晴朗"));
    verify(memoryService).remember(eq("[通知] 发送到 lark: 测试消息：今日天气晴朗"), eq(MemoryScope.ARCHIVAL));
  }

  @Test
  @DisplayName("渠道不在全局注册表中时明确报错且不保存记忆")
  void execute_unknownChannelFails() {
    when(channelStore.getByName("nonexistent")).thenReturn(null);

    ToolExecutionResult result =
        notifyTools.execute("{\"channel\":\"nonexistent\",\"content\":\"测试消息\"}");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains("not found");
  }

  @Test
  @DisplayName("渠道 URL 为空时明确报错")
  void execute_emptyUrlFails() {
    when(channelStore.getByName("ops"))
        .thenReturn(new NotifyChannelView(2L, "ops", "webhook", "", ""));

    ToolExecutionResult result = notifyTools.execute("{\"channel\":\"ops\",\"content\":\"测试消息\"}");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains("empty URL");
  }

  @Test
  @DisplayName("超长通知内容写入记忆时截断")
  void execute_truncatesLongContentInMemory() {
    when(channelStore.getByName("lark"))
        .thenReturn(
            new NotifyChannelView(1L, "lark", "webhook", "https://lark.example.com/webhook", ""));
    String longContent = "长".repeat(600);

    ToolExecutionResult result =
        notifyTools.execute("{\"channel\":\"lark\",\"content\":\"" + longContent + "\"}");

    assertThat(result.success()).isTrue();
    ArgumentCaptor<String> memoryContent = ArgumentCaptor.forClass(String.class);
    verify(memoryService).remember(memoryContent.capture(), eq(MemoryScope.ARCHIVAL));
    assertThat(memoryContent.getValue().length()).isLessThan(540);
    assertThat(memoryContent.getValue()).endsWith("…");
  }

  @Test
  @DisplayName("工具名称和描述正确")
  void toolMetadataIsCorrect() {
    assertThat(notifyTools.name()).isEqualTo("notify");
    assertThat(notifyTools.description()).contains("Send notification").contains("save to memory");
  }
}

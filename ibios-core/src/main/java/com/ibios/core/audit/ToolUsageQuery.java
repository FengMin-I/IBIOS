package com.ibios.core.audit;

import java.util.List;
import java.util.Map;

/**
 * 只读聚合：每个 Agent 历史上实际调用过哪些工具。
 *
 * <p>工具本身是全局注册表，所有 Agent 可见集合相同；这里的答案是「用过」而不是「可用」。
 */
public interface ToolUsageQuery {

  /**
   * @return key 为 profile（Agent）名，value 为该 Agent 用过的工具名，去重且按名字排序
   */
  Map<String, List<String>> findUsedToolNamesByProfile();
}

package com.ibios.core.tool;

/** 人机交互接口。不同渠道挂不同实现：CLI 读终端，定时任务/Web 报不支持。 */
public interface UserInteraction {
  /**
   * 向用户提问并获取回答。
   *
   * @param question 问题文本
   * @return 用户回答（单行文本）
   * @throws UnsupportedOperationException 如果当前渠道不支持交互
   */
  String ask(String question);
}

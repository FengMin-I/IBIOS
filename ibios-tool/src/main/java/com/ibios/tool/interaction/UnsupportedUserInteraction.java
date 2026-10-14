package com.ibios.tool.interaction;

import com.ibios.core.tool.UserInteraction;

/** 无人值守渠道的交互实现：直接抛异常，表示不支持交互。 */
public class UnsupportedUserInteraction implements UserInteraction {

  @Override
  public String ask(String question) {
    throw new UnsupportedOperationException("User interaction not supported in this channel");
  }
}

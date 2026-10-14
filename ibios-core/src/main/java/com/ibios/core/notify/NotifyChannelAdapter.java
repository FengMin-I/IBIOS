package com.ibios.core.notify;

public interface NotifyChannelAdapter {

  void send(NotifyTarget target, String content);
}

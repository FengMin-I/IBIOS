package com.ibios.core.notify;

import java.util.List;

public interface NotifyChannelStore {

  List<NotifyChannelView> listAll();

  NotifyChannelView getById(Long id);

  NotifyChannelView getByName(String name);

  NotifyChannelView create(String name, String type, String url, String description);

  NotifyChannelView update(Long id, String name, String type, String url, String description);

  void delete(Long id);
}

package com.ibios.core.sandbox;

import java.util.List;

/** 沙箱白名单的持久化端口。启动时用配置补齐，运行时增删写穿，沙箱按内存快照即时生效。 */
public interface SandboxWhitelistStore {

  List<WhitelistEntryView> listAll();

  WhitelistEntryView findById(Long id);

  boolean exists(WhitelistType type, String value);

  /** 同 (type, value) 已存在时由唯一约束拒绝。 */
  WhitelistEntryView create(WhitelistType type, String value);

  boolean delete(Long id);
}

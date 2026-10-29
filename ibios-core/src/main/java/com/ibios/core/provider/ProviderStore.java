package com.ibios.core.provider;

import java.util.List;

/** Provider 配置的持久化端口。运行时按名取配置动态建 ChatModel，改完即时生效。 */
public interface ProviderStore {

  List<ProviderView> listAll();

  ProviderView getById(Long id);

  ProviderView getByName(String name);

  ProviderView create(String name, String type, String apiKey, String baseUrl, String model);

  /** name 即 Agent 的引用键，不可改；其余字段可改。 */
  ProviderView update(Long id, String type, String apiKey, String baseUrl, String model);

  void delete(Long id);

  boolean isEmpty();
}

package com.ibios.core.tool;

import java.util.List;

/** 网页搜索提供者接口。核心阶段实现 DuckDuckGo，扩展阶段可加 Google/Bing/Tavily。 */
public interface SearchProvider {

  /**
   * 搜索结果记录。
   *
   * @param title 标题
   * @param url URL
   * @param snippet 摘要
   */
  record SearchResult(String title, String url, String snippet) {}

  /**
   * 执行搜索。
   *
   * @param query 搜索查询
   * @return 搜索结果列表（无结果返回空列表，不抛异常）
   */
  List<SearchResult> search(String query);
}

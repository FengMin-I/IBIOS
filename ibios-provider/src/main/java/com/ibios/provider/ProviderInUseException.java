package com.ibios.provider;

/** Provider 被某个 Agent 引用时不允许删除。 */
public class ProviderInUseException extends RuntimeException {

  public ProviderInUseException(String providerName, String agentName) {
    super("Provider " + providerName + " is still used by agent: " + agentName);
  }
}

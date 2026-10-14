package com.ibios.core.profile;

public class ProviderNotFoundException extends RuntimeException {

  public ProviderNotFoundException(String providerName) {
    super("Provider not found: " + providerName);
  }
}

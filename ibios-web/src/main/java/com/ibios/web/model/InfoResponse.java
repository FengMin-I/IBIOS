package com.ibios.web.model;

import java.util.List;

public record InfoResponse(String version, List<ProviderStatus> providers) {

  public record ProviderStatus(String name, boolean connected) {}
}

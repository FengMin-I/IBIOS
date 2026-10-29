package com.ibios.core.provider;

public record ProviderView(
    Long id, String name, String type, String apiKey, String baseUrl, String model) {}

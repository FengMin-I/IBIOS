package com.ibios.core.notify;

public record NotifyChannelView(
    Long id, String name, String type, String url, String description) {}

package com.ibios.core.notify;

import java.util.Map;

public record NotifyTarget(String channelType, Map<String, String> config) {}

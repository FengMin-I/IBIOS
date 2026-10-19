package com.ibios.web.model;

import java.util.List;

public record SessionView(String sessionId, String profileName, List<MessageResponse> messages) {}

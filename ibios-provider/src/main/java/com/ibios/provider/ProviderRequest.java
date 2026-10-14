package com.ibios.provider;

import com.ibios.core.tool.IbiosTool;
import java.util.List;

public record ProviderRequest(String content, List<IbiosTool> availableTools) {}

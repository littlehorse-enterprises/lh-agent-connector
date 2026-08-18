package io.littlehorse.agent.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ToolsManager {

    private final McpToolProvider mcpToolProvider;
    private final AtomicReference<Map<String, AiServiceTool>> toolsCache = new AtomicReference<>();
    private final AtomicBoolean isCacheValid = new AtomicBoolean(false);

    public ToolsManager(McpToolProvider mcpToolProvider) {
        this.mcpToolProvider = mcpToolProvider;
    }

    public void invalidateCache() {
        isCacheValid.set(false);
    }

    // Needed to reload cache when it's invalidated by MCP notifications/tools/list_changed
    // it's synchronized so that multiple threads don't try to reload the cache at the same time
    private synchronized void loadTools() {
        List<AiServiceTool> tools = mcpToolProvider.provideTools(null).aiServiceTools();
        LinkedHashMap<String, AiServiceTool> toolMap = new LinkedHashMap<>();
        for (AiServiceTool tool : tools) {
            if (toolMap.putIfAbsent(tool.name(), tool) != null) {
                throw new IllegalArgumentException(
                        "Multiple MCP clients expose the tool name '" + tool.name() + "'");
            }
        }
        toolsCache.set(Collections.unmodifiableMap(toolMap));
        isCacheValid.set(true);
    }

    private void checkCache() {
        if (!isCacheValid.get()) {
            loadTools();
        }
    }

    public List<ToolSpecification> getToolsSpecification() {
        checkCache();
        return toolsCache.get().values().stream()
                .map(AiServiceTool::toolSpecification)
                .toList();
    }

    public Optional<ToolExecutor> getToolExecutor(String name) {
        checkCache();
        return Optional.ofNullable(toolsCache.get().get(name)).map(AiServiceTool::toolExecutor);
    }
}

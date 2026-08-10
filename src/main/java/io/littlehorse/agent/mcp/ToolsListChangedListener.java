package io.littlehorse.agent.mcp;

import dev.langchain4j.mcp.client.McpClientListener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ToolsListChangedListener implements McpClientListener {

    private final Instance<ToolsManager> toolsManager;

    public ToolsListChangedListener(Instance<ToolsManager> toolsManager) {
        this.toolsManager = toolsManager;
    }

    @Override
    public void onNotificationToolsListChanged() {
        toolsManager.get().invalidateCache();
    }
}

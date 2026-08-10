package io.littlehorse.agent.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.McpConfiguration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import java.util.Map;

@ApplicationScoped
public class McpToolProviderProducer {

    private final McpToolPolicy toolPolicy;

    public McpToolProviderProducer(McpToolPolicy toolPolicy) {
        this.toolPolicy = toolPolicy;
    }

    @Singleton
    public McpToolProvider mcpToolProvider(
            AgentConfiguration agentConfiguration, Map<String, DefaultMcpClient> mcpClients) {
        McpConfiguration configuration = agentConfiguration.mcp();
        return McpToolProvider.builder()
                .mcpClients(mcpClients.values().toArray(new DefaultMcpClient[0]))
                // Partial tooling requires coordinated MCP client and tool-provider changes.
                .failIfOneServerFails(true)
                .filter(toolPolicy.filter(configuration.clients()))
                .toolSpecificationMapper(toolPolicy.mapper(configuration.clients()))
                .build();
    }
}

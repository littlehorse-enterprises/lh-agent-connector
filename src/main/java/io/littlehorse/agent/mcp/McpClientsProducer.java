package io.littlehorse.agent.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClientListener;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.McpConfiguration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class McpClientsProducer {

    private final McpClientFactory clientFactory;

    public McpClientsProducer(McpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Singleton
    public Map<String, DefaultMcpClient> mcpClients(
            AgentConfiguration agentConfiguration,
            Map<String, Supplier<String>> mcpAuthSuppliers,
            McpClientListener toolsListChangedListener) {
        McpConfiguration configuration = agentConfiguration.mcp();
        return configuration.clients().entrySet().stream()
                .filter(entry -> entry.getValue().enabled())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.entry(
                        entry.getKey(),
                        clientFactory.create(
                                entry.getKey(),
                                entry.getValue(),
                                Optional.ofNullable(mcpAuthSuppliers.get(entry.getKey())),
                                toolsListChangedListener)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

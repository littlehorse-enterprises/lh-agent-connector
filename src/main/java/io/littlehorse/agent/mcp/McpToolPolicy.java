package io.littlehorse.agent.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;

import io.littlehorse.agent.configuration.McpConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@ApplicationScoped
public class McpToolPolicy {

    public BiFunction<McpClient, ToolSpecification, ToolSpecification> mapper(
            Map<String, McpConfiguration.Client> clients) {
        return (client, tool) -> {
            Optional<McpConfiguration.Tools> tools = Optional.ofNullable(clients.get(client.key()))
                    .map(McpConfiguration.Client::tools);
            String defaultNamePrefix = client.key() + "_";
            String namePrefix =
                    tools.flatMap(McpConfiguration.Tools::namePrefix).orElse(defaultNamePrefix);

            ToolSpecification.Builder builder = tool.toBuilder();

            tools.map(McpConfiguration.Tools::specificationMapping)
                    .map(mapping -> mapping.get(tool.name()))
                    .flatMap(McpConfiguration.SpecificationMapping::name)
                    .ifPresentOrElse(builder::name, () -> builder.name(namePrefix + tool.name()));

            tools.map(McpConfiguration.Tools::specificationMapping)
                    .map(mapping -> mapping.get(tool.name()))
                    .flatMap(McpConfiguration.SpecificationMapping::description)
                    .ifPresent(builder::description);

            return builder.build();
        };
    }

    public BiPredicate<McpClient, ToolSpecification> filter(
            Map<String, McpConfiguration.Client> clients) {
        return (client, tool) -> {
            Optional<McpConfiguration.Tools> tools = Optional.ofNullable(clients.get(client.key()))
                    .map(McpConfiguration.Client::tools);
            McpConfiguration.ToolMode mode =
                    tools.map(McpConfiguration.Tools::mode).orElse(McpConfiguration.ToolMode.ALL);

            if (mode.equals(McpConfiguration.ToolMode.ALL)) {
                return true;
            }

            if (mode.equals(McpConfiguration.ToolMode.INCLUDE)) {
                return tools.flatMap(McpConfiguration.Tools::names)
                        .map(names -> names.contains(tool.name()))
                        .orElse(false);
            }

            return tools.flatMap(McpConfiguration.Tools::names)
                    .map(names -> !names.contains(tool.name()))
                    .orElse(true);
        };
    }
}

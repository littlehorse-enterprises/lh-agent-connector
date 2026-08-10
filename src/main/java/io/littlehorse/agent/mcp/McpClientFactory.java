package io.littlehorse.agent.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.websocket.WebSocketMcpTransport;

import io.littlehorse.agent.configuration.McpConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@ApplicationScoped
public class McpClientFactory {

    public DefaultMcpClient create(
            String name,
            McpConfiguration.Client configuration,
            Optional<Supplier<String>> authSupplier,
            McpClientListener toolsListChangedListener) {
        if (!configuration.auth().type().equals(McpConfiguration.AuthenticationType.NONE)
                && authSupplier.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing authentication supplier for MCP client: " + name);
        }
        McpTransport transport = createTransport(authSupplier, configuration);
        return DefaultMcpClient.builder()
                .key(name)
                .clientName("lh-agent-" + name)
                .clientVersion("1.0")
                .transport(transport)
                .cacheToolList(true)
                .initializationTimeout(configuration.initializationTimeout())
                .toolExecutionTimeout(configuration.toolExecutionTimeout())
                .addListener(toolsListChangedListener)
                .build();
    }

    private McpTransport createTransport(
            Optional<Supplier<String>> authSupplier, McpConfiguration.Client configuration) {
        return switch (configuration.transport()) {
            case STREAMABLE_HTTP -> streamableHttp(authSupplier, configuration);
            case SSE -> sse(authSupplier, configuration);
            case WEBSOCKET -> webSocket(authSupplier, configuration);
        };
    }

    private McpTransport streamableHttp(
            Optional<Supplier<String>> authSupplier, McpConfiguration.Client configuration) {
        return StreamableHttpMcpTransport.builder()
                .url(configuration.url())
                .timeout(configuration.timeout())
                .logRequests(configuration.logRequests())
                .logResponses(configuration.logResponses())
                .followRedirects(configuration.followRedirects())
                .subsidiaryChannel(configuration.subsidiaryChannel())
                .customHeaders(headerSupplier(configuration.headers(), authSupplier))
                .build();
    }

    @SuppressWarnings("removal")
    private McpTransport sse(
            Optional<Supplier<String>> authSupplier, McpConfiguration.Client configuration) {
        return HttpMcpTransport.builder()
                .sseUrl(configuration.url())
                .timeout(configuration.timeout())
                .logRequests(configuration.logRequests())
                .logResponses(configuration.logResponses())
                .customHeaders(headerSupplier(configuration.headers(), authSupplier))
                .build();
    }

    private McpTransport webSocket(
            Optional<Supplier<String>> authSupplier, McpConfiguration.Client configuration) {
        return WebSocketMcpTransport.builder()
                .url(configuration.url())
                .timeout(configuration.timeout())
                .logRequests(configuration.logRequests())
                .logResponses(configuration.logResponses())
                .headersSupplier(headerSupplier(configuration.headers(), authSupplier))
                .build();
    }

    private McpHeadersSupplier headerSupplier(
            Map<String, String> headers, Optional<Supplier<String>> authSupplier) {
        return _ -> {
            Map<String, String> suppliedHeaders = new HashMap<>(Map.copyOf(headers));
            authSupplier.ifPresent(
                    supplier -> suppliedHeaders.put("Authorization", "Bearer " + supplier.get()));
            return suppliedHeaders;
        };
    }
}

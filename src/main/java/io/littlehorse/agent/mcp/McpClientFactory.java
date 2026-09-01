package io.littlehorse.agent.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.websocket.WebSocketMcpTransport;

import io.littlehorse.agent.configuration.McpConfiguration;
import io.quarkiverse.langchain4j.mcp.auth.McpClientAuthProvider;
import io.quarkiverse.langchain4j.mcp.runtime.http.QuarkusHttpMcpTransport;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@ApplicationScoped
public class McpClientFactory {

    private static final McpClientAuthProvider NO_AUTH_PROVIDER = _ -> null;

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

    private McpTransport sse(
            Optional<Supplier<String>> authSupplier, McpConfiguration.Client configuration) {
        McpHeadersSupplier headers = headerSupplier(configuration.headers(), authSupplier);
        return new QuarkusHttpMcpTransport.Builder()
                .sseUrl(configuration.url())
                .timeout(configuration.timeout())
                .logRequests(configuration.logRequests())
                .logResponses(configuration.logResponses())
                .mcpClientAuthProvider(NO_AUTH_PROVIDER)
                .headers(() -> headers.apply(null))
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
            List<McpConfiguration.Header> headers, Optional<Supplier<String>> authSupplier) {
        return _ -> {
            Map<String, String> suppliedHeaders = new LinkedHashMap<>();
            headers.forEach(header -> suppliedHeaders.put(header.name(), header.value()));
            authSupplier.ifPresent(
                    supplier -> suppliedHeaders.put("Authorization", "Bearer " + supplier.get()));
            return suppliedHeaders;
        };
    }
}

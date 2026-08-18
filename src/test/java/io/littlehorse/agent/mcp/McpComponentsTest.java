package io.littlehorse.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.service.tool.ToolExecutionResult;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.McpConfiguration;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class McpComponentsTest {

    private static final Pattern REQUEST_ID = Pattern.compile("\\\"id\\\":(\\d+)");

    @Test
    void mcpClientsCreatesOnlyEnabledClientsWithTheirAuthenticationSupplier() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        McpClientListener listener = mock(McpClientListener.class);
        HttpServer server = mcpServer(authorization);
        String url = "http://localhost:" + server.getAddress().getPort() + "/mcp";
        McpConfiguration configuration = new TestMcpConfiguration(Map.of(
                "enabled", new TestClientConfiguration(true, url, allTools()),
                "disabled", new TestClientConfiguration(false, url, allTools())));

        Map<String, DefaultMcpClient> clients = Map.of();
        try {
            clients = mcpClientsProducer()
                    .mcpClients(
                            agentConfiguration(configuration),
                            Map.of("enabled", () -> "test-token"),
                            listener);

            assertThat(clients).containsOnlyKeys("enabled");
            assertThat(clients.get("enabled").key()).isEqualTo("enabled");
            assertThat(authorization).hasValue("Bearer test-token");
            clients.get("enabled").listTools();
            verify(listener).beforeToolsList(any(McpCallContext.class));
            assertThatThrownBy(clients::clear).isInstanceOf(UnsupportedOperationException.class);
        } finally {
            clients.values().forEach(DefaultMcpClient::close);
            server.stop(0);
        }
    }

    @Test
    void mcpToolProviderAppliesEveryToolModeMapsSpecificationsAndExecutesOriginalToolNames()
            throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> executedTool = new AtomicReference<>();
        HttpServer server = mcpServer(authorization, executedTool);
        String url = "http://localhost:" + server.getAddress().getPort() + "/mcp";
        McpConfiguration configuration = new TestMcpConfiguration(Map.of(
                "all", new TestClientConfiguration(true, url, allTools()),
                "include",
                        new TestClientConfiguration(
                                true,
                                url,
                                new TestTools(
                                        McpConfiguration.ToolMode.INCLUDE,
                                        Optional.of(List.of("lookup")),
                                        Optional.empty(),
                                        Map.of(
                                                "lookup",
                                                new TestSpecificationMapping(
                                                        Optional.empty(),
                                                        Optional.of(
                                                                "Mapped lookup description"))))),
                "exclude",
                        new TestClientConfiguration(
                                true,
                                url,
                                new TestTools(
                                        McpConfiguration.ToolMode.EXCLUDE,
                                        Optional.of(List.of("delete")),
                                        Optional.of("custom_"),
                                        Map.of()))));
        Map<String, DefaultMcpClient> clients = Map.of();

        try {
            clients = mcpClientsProducer()
                    .mcpClients(
                            agentConfiguration(configuration),
                            Map.of(
                                    "all", () -> "test-token",
                                    "include", () -> "test-token",
                                    "exclude", () -> "test-token"),
                            mock(McpClientListener.class));
            McpToolProvider provider = new McpToolProviderProducer(new McpToolPolicy())
                    .mcpToolProvider(agentConfiguration(configuration), clients);
            ToolsManager toolsManager = new ToolsManager(provider);

            assertThat(toolsManager.getToolsSpecification())
                    .extracting(specification -> specification.name())
                    .containsExactlyInAnyOrder(
                            "all_lookup", "all_delete", "include_lookup", "custom_lookup");
            assertThat(toolsManager.getToolsSpecification())
                    .filteredOn(specification -> specification.name().equals("include_lookup"))
                    .singleElement()
                    .extracting(specification -> specification.description())
                    .isEqualTo("Mapped lookup description");

            ToolExecutionResult result = toolsManager
                    .getToolExecutor("include_lookup")
                    .orElseThrow()
                    .executeWithContext(
                            ToolExecutionRequest.builder()
                                    .id("call-1")
                                    .name("include_lookup")
                                    .arguments("{\"query\":\"horse\"}")
                                    .build(),
                            InvocationContext.builder().build());

            assertThat(executedTool).hasValue("lookup");
            assertThat(result.resultText()).isEqualTo("MCP result");
            assertThat(result.isError()).isFalse();
        } finally {
            clients.values().forEach(DefaultMcpClient::close);
            server.stop(0);
        }
    }

    @Test
    void mcpClientsRejectsAnEnabledClientWithoutAnAuthenticationSupplier() {
        McpConfiguration configuration = new TestMcpConfiguration(Map.of(
                "missing-auth",
                new TestClientConfiguration(true, "https://mcp.example.test/mcp", allTools())));

        assertThatThrownBy(() -> mcpClientsProducer()
                        .mcpClients(
                                agentConfiguration(configuration),
                                Map.of(),
                                mock(McpClientListener.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing authentication supplier for MCP client: missing-auth");
    }

    @Test
    void mcpClientsSupportsUnauthenticatedClients() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = mcpServer(authorization);
        String url = "http://localhost:" + server.getAddress().getPort() + "/mcp";
        McpConfiguration configuration = new TestMcpConfiguration(Map.of(
                "public",
                new TestClientConfiguration(
                        true,
                        url,
                        allTools(),
                        new TestAuthentication(
                                McpConfiguration.AuthenticationType.NONE,
                                Optional.empty(),
                                Optional.empty()))));
        Map<String, DefaultMcpClient> clients = Map.of();

        try {
            clients = mcpClientsProducer()
                    .mcpClients(
                            agentConfiguration(configuration),
                            Map.of(),
                            mock(McpClientListener.class));

            assertThat(clients).containsOnlyKeys("public");
            assertThat(authorization).hasValue(null);
        } finally {
            clients.values().forEach(DefaultMcpClient::close);
            server.stop(0);
        }
    }

    @Test
    void toolsListChangedListenerInvalidatesTheToolsManagerCache() {
        ToolsManager toolsManager = mock(ToolsManager.class);
        @SuppressWarnings("unchecked")
        Instance<ToolsManager> toolsManagerInstance = mock(Instance.class);
        when(toolsManagerInstance.get()).thenReturn(toolsManager);

        McpClientListener listener = new ToolsListChangedListener(toolsManagerInstance);
        listener.onNotificationToolsListChanged();

        verify(toolsManager).invalidateCache();
    }

    private static McpClientsProducer mcpClientsProducer() {
        return new McpClientsProducer(new McpClientFactory());
    }

    private static HttpServer mcpServer(AtomicReference<String> authorization) throws IOException {
        return mcpServer(authorization, new AtomicReference<>());
    }

    private static HttpServer mcpServer(
            AtomicReference<String> authorization, AtomicReference<String> executedTool)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
                "/mcp", exchange -> respondToMcpRequest(exchange, authorization, executedTool));
        server.start();
        return server;
    }

    private static void respondToMcpRequest(
            HttpExchange exchange,
            AtomicReference<String> authorization,
            AtomicReference<String> executedTool)
            throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        String request = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String id = requestId(request);
        String response;
        if (request.contains("\"method\":\"initialize\"")) {
            response = """
                    {"jsonrpc":"2.0","id":%s,"result":{"protocolVersion":"2025-11-25","capabilities":{"tools":{"listChanged":false}},"serverInfo":{"name":"test-server","version":"1.0"}}}
                    """.formatted(id);
        } else if (request.contains("\"method\":\"tools/list\"")) {
            response = """
                    {"jsonrpc":"2.0","id":%s,"result":{"tools":[{"name":"lookup","description":"Original lookup description","inputSchema":{"type":"object","properties":{"query":{"type":"string"}}}},{"name":"delete","description":"Delete data","inputSchema":{"type":"object","properties":{}}}]}}
                    """.formatted(id);
        } else if (request.contains("\"method\":\"tools/call\"")) {
            executedTool.set(request.contains("\"name\":\"lookup\"") ? "lookup" : "delete");
            response = """
                    {"jsonrpc":"2.0","id":%s,"result":{"content":[{"type":"text","text":"MCP result"}],"isError":false}}
                    """.formatted(id);
        } else {
            response = "{}";
        }
        byte[] responseBytes = response.getBytes(UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String requestId(String request) {
        Matcher matcher = REQUEST_ID.matcher(request);
        return matcher.find() ? matcher.group(1) : "null";
    }

    private static McpConfiguration.Tools allTools() {
        return new TestTools(
                McpConfiguration.ToolMode.ALL, Optional.empty(), Optional.empty(), Map.of());
    }

    private static AgentConfiguration agentConfiguration(McpConfiguration mcpConfiguration) {
        AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
        when(agentConfiguration.mcp()).thenReturn(mcpConfiguration);
        return agentConfiguration;
    }

    private record TestMcpConfiguration(Map<String, McpConfiguration.Client> clients)
            implements McpConfiguration {}

    private record TestClientConfiguration(
            boolean enabled,
            String url,
            McpConfiguration.Tools tools,
            McpConfiguration.Authentication auth)
            implements McpConfiguration.Client {

        private TestClientConfiguration(boolean enabled, String url, McpConfiguration.Tools tools) {
            this(
                    enabled,
                    url,
                    tools,
                    new TestAuthentication(
                            McpConfiguration.AuthenticationType.BEARER,
                            Optional.of("test-token"),
                            Optional.empty()));
        }

        @Override
        public McpConfiguration.Transport transport() {
            return McpConfiguration.Transport.STREAMABLE_HTTP;
        }

        @Override
        public Map<String, String> headers() {
            return Map.of("x-test", "configured-header");
        }

        @Override
        public Duration timeout() {
            return Duration.ofSeconds(5);
        }

        @Override
        public Duration initializationTimeout() {
            return Duration.ofSeconds(5);
        }

        @Override
        public Duration toolExecutionTimeout() {
            return Duration.ofSeconds(5);
        }

        @Override
        public boolean logRequests() {
            return false;
        }

        @Override
        public boolean logResponses() {
            return false;
        }

        @Override
        public boolean followRedirects() {
            return true;
        }

        @Override
        public boolean subsidiaryChannel() {
            return false;
        }
    }

    private record TestTools(
            McpConfiguration.ToolMode mode,
            Optional<List<String>> names,
            Optional<String> namePrefix,
            Map<String, McpConfiguration.SpecificationMapping> specificationMapping)
            implements McpConfiguration.Tools {}

    private record TestSpecificationMapping(Optional<String> name, Optional<String> description)
            implements McpConfiguration.SpecificationMapping {}

    private record TestAuthentication(
            McpConfiguration.AuthenticationType type,
            Optional<String> token,
            Optional<String> oidcClient)
            implements McpConfiguration.Authentication {}
}

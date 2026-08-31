package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfigurationWithEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.mcp.client.DefaultMcpClient;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

@QuarkusTest
class McpConfigurationTest {

    @Inject
    AgentConfiguration agentConfiguration;

    @Inject
    Map<String, DefaultMcpClient> mcpClients;

    @Test
    void mapsMultipleClientSettingsFromApplicationProperties() {
        McpConfiguration configuration = agentConfiguration.mcp();
        McpConfiguration.Client tokenClient = configuration.clients().get("github");
        assertThat(tokenClient.auth().type()).isEqualTo(McpConfiguration.AuthenticationType.BEARER);
        assertThat(tokenClient.auth().token()).contains("test-github-token");
        assertThat(tokenClient.auth().oidcClient()).isEmpty();

        McpConfiguration.Client client = configuration.clients().get("test-server");
        assertThat(client.enabled()).isFalse();
        assertThat(client.transport()).isEqualTo(McpConfiguration.Transport.STREAMABLE_HTTP);
        assertThat(client.url())
                .isEqualTo(URI.create("https://mcp.example.test/mcp").toString());
        assertThat(client.auth().type()).isEqualTo(McpConfiguration.AuthenticationType.OAUTH2);
        assertThat(client.auth().oidcClient()).contains("test-oauth");
        assertThat(client.auth().token()).isEmpty();
        assertThat(client.headers())
                .extracting(McpConfiguration.Header::name)
                .containsExactly("X-Test", "X-API-Key");
        assertThat(client.headers())
                .extracting(McpConfiguration.Header::value)
                .containsExactly("test-header", "test-api-key");
        assertThat(configuration.clients().get("github").headers()).isEmpty();

        McpConfiguration.Tools tools = client.tools();
        assertThat(tools.mode()).isEqualTo(McpConfiguration.ToolMode.INCLUDE);
        assertThat(tools.names()).contains(List.of("lookup", "create"));
        assertThat(tools.namePrefix()).contains("test_");
        assertThat(tools.specificationMapping().get("lookup").name()).contains("search");
        assertThat(tools.specificationMapping().get("lookup").description())
                .contains("Search test data.");
    }

    @Test
    void mcpClientsExcludesDisabledClients() {
        assertThat(mcpClients).isEmpty();
    }

    @Test
    void mapsHeaderNamesFromEnvironmentVariableValues() {
        AgentConfiguration configuration = buildConfigurationWithEnvironment(
                Map.of("agent.mcp.clients.remote.url", "https://mcp.example.test/mcp"),
                Map.of(
                        "AGENT_MCP_CLIENTS_REMOTE_HEADERS_0__NAME",
                        "X-API-Key",
                        "AGENT_MCP_CLIENTS_REMOTE_HEADERS_0__VALUE",
                        "environment-secret"));

        assertThat(configuration.mcp().clients().get("remote").headers())
                .singleElement()
                .satisfies(header -> {
                    assertThat(header.name()).isEqualTo("X-API-Key");
                    assertThat(header.value()).isEqualTo("environment-secret");
                });
    }
}

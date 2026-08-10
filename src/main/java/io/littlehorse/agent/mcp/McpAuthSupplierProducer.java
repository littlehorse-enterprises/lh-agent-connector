package io.littlehorse.agent.mcp;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.McpConfiguration;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.TokensHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class McpAuthSupplierProducer {

    @Singleton
    public Map<String, Supplier<String>> mcpAuthSuppliers(
            AgentConfiguration agentConfiguration, OidcClients oidcClients) {
        McpConfiguration configuration = agentConfiguration.mcp();
        return configuration.clients().entrySet().stream()
                .filter(entry -> entry.getValue().enabled())
                .filter(entry -> !entry.getValue()
                        .auth()
                        .type()
                        .equals(McpConfiguration.AuthenticationType.NONE))
                .map(entry -> Map.entry(
                        entry.getKey(), authSupplier(entry.getValue().auth(), oidcClients)))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Supplier<String> authSupplier(
            McpConfiguration.Authentication authentication, OidcClients oidcClients) {
        return switch (authentication.type()) {
            case BEARER ->
                () -> authentication
                        .token()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Missing token for BEARER authentication"));
            case OAUTH2 -> {
                String oidcClientName = authentication.oidcClient().orElseThrow();
                OidcClient client = oidcClients.getClient(oidcClientName);
                if (client == null) {
                    throw new IllegalArgumentException("Unknown OIDC client: " + oidcClientName);
                }
                yield getTokenSupplier(new TokensHelper(), client);
            }
            case NONE ->
                throw new IllegalArgumentException(
                        "No authentication supplier exists for auth type NONE");
        };
    }

    private Supplier<String> getTokenSupplier(TokensHelper helper, OidcClient client) {
        return () ->
                helper.getTokens(client).await().atMost(Duration.ofSeconds(30)).getAccessToken();
    }
}

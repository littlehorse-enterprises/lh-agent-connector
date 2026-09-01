package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfiguration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.ConfigValidationException;

import org.junit.jupiter.api.Test;

import java.util.Map;

class McpConfigurationValidationTest {

    private static final String AUTHENTICATION_VALIDATION_MESSAGE =
            "authentication settings must match auth.type";

    @Test
    void allowsAbsentOptionalToolStrings() {
        assertThatCode(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token")))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDescriptionOnlySpecificationMapping() {
        assertThatCode(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.specification-mapping.lookup.description",
                        "Looks up remote data")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankOptionalToolStrings() {
        assertThatThrownBy(() -> buildConfiguration(
                        configurationWith("agent.mcp.clients.remote.tools.name-prefix", "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> buildConfiguration(configurationWith(
                        "agent.mcp.clients.remote.tools.specification-mapping.lookup.name", "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> buildConfiguration(configurationWith(
                        "agent.mcp.clients.remote.tools.specification-mapping.lookup.description",
                        "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsBlankHeaderNamesAndValues() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.headers[0].name",
                        "   ",
                        "agent.mcp.clients.remote.headers[0].value",
                        "value")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.headers[0].name",
                        "X-Test",
                        "agent.mcp.clients.remote.headers[0].value",
                        "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsTheLegacyHeaderMapSyntax() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.headers.x-test",
                        "value")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.mcp.clients.remote.headers.x-test");
    }

    @Test
    void rejectsNamesWhenToolModeIsAll() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.names",
                        "lookup")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Names are not allowed when mode is ALL");
    }

    @Test
    void requiresNamesWhenToolModeIsIncludeOrExclude() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.mode",
                        "include")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "at least one name must be provided when mode is INCLUDE or EXCLUDE");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.mode",
                        "exclude")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "at least one name must be provided when mode is INCLUDE or EXCLUDE");
    }

    @Test
    void allowsNamesWhenToolModeIsIncludeOrExclude() {
        assertThatCode(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.mode",
                        "include",
                        "agent.mcp.clients.remote.tools.names",
                        "lookup")))
                .doesNotThrowAnyException();

        assertThatCode(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.tools.mode",
                        "exclude",
                        "agent.mcp.clients.remote.tools.names",
                        "delete")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnsupportedUrlScheme() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.transport",
                        "websocket",
                        "agent.mcp.clients.remote.url",
                        "ftp://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "oauth2",
                        "agent.mcp.clients.remote.auth.oidc-client",
                        "test-oauth")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be an HTTP(S) or WS(S) URL");
    }

    @Test
    void rejectsNegativeDurations() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "oauth2",
                        "agent.mcp.clients.remote.auth.oidc-client",
                        "test-oauth",
                        "agent.mcp.clients.remote.timeout",
                        "-PT1S")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void allowsNoAuthenticationByDefault() {
        assertThatCode(() -> buildConfiguration(
                        Map.of("agent.mcp.clients.remote.url", "https://mcp.example.test/mcp")))
                .doesNotThrowAnyException();
    }

    @Test
    void requiresTheSettingForTheSelectedAuthenticationType() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(AUTHENTICATION_VALIDATION_MESSAGE);

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "oauth2")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(AUTHENTICATION_VALIDATION_MESSAGE);
    }

    @Test
    void requiresANotBlankAuthenticationSource() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "    ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "oauth2",
                        "agent.mcp.clients.remote.auth.oidc-client",
                        "    ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsAuthenticationSettingsForAnotherType() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.auth.oidc-client",
                        "test-oauth")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(AUTHENTICATION_VALIDATION_MESSAGE);
    }

    @Test
    void rejectsAnAuthorizationHeaderForManagedAuthentication() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "bearer",
                        "agent.mcp.clients.remote.auth.token",
                        "direct-token",
                        "agent.mcp.clients.remote.headers[0].name",
                        "authorization",
                        "agent.mcp.clients.remote.headers[0].value",
                        "Bearer another-token")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "Authorization header cannot be configured when auth.type is BEARER or OAUTH2");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.mcp.clients.remote.url",
                        "https://mcp.example.test/mcp",
                        "agent.mcp.clients.remote.auth.type",
                        "oauth2",
                        "agent.mcp.clients.remote.auth.oidc-client",
                        "test-oauth",
                        "agent.mcp.clients.remote.headers[0].name",
                        "AUTHORIZATION",
                        "agent.mcp.clients.remote.headers[0].value",
                        "Bearer another-token")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "Authorization header cannot be configured when auth.type is BEARER or OAUTH2");
    }

    private static Map<String, String> configurationWith(String key, String value) {
        return Map.of(
                "agent.mcp.clients.remote.url",
                "https://mcp.example.test/mcp",
                "agent.mcp.clients.remote.auth.type",
                "bearer",
                "agent.mcp.clients.remote.auth.token",
                "direct-token",
                key,
                value);
    }
}

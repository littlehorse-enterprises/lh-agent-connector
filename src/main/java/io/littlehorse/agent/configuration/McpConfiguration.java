package io.littlehorse.agent.configuration;

import io.littlehorse.agent.configuration.validation.NotBlankIfPresent;
import io.smallrye.config.WithDefault;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.time.DurationMin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application configuration for the MCP servers exposed to the agent.
 */
public interface McpConfiguration {

    Map<@NotBlank String, Client> clients();

    interface Client {

        @WithDefault("true")
        boolean enabled();

        @WithDefault("streamable-http")
        Transport transport();

        @NotBlank(message = "must be configured")
        @Pattern(
                regexp = "(?i)^https?://\\S+$|^wss?://\\S+$",
                message = "must be an HTTP(S) or WS(S) URL")
        String url();

        Map<@NotBlank String, @NotBlank String> headers();

        Authentication auth();

        @AssertTrue(
                message =
                        "the Authorization header cannot be configured when auth.type is BEARER or OAUTH2")
        default boolean isAuthorizationHeaderValid() {
            return auth().type().equals(AuthenticationType.NONE)
                    || headers().keySet().stream()
                            .noneMatch(header -> header.equalsIgnoreCase("Authorization"));
        }

        @WithDefault("PT30S")
        @DurationMin(message = "must be greater than zero")
        Duration timeout();

        @WithDefault("PT30S")
        @DurationMin(message = "must be greater than zero")
        Duration initializationTimeout();

        @WithDefault("PT60S")
        @DurationMin(message = "must not be negative")
        Duration toolExecutionTimeout();

        @WithDefault("false")
        boolean logRequests();

        @WithDefault("false")
        boolean logResponses();

        @WithDefault("true")
        boolean followRedirects();

        @WithDefault("false")
        boolean subsidiaryChannel();

        Tools tools();
    }

    interface Authentication {

        @WithDefault("none")
        AuthenticationType type();

        Optional<@NotBlankIfPresent String> token();

        Optional<@NotBlankIfPresent String> oidcClient();

        @AssertTrue(message = "authentication settings must match auth.type")
        default boolean isValid() {
            return switch (type()) {
                case NONE -> token().isEmpty() && oidcClient().isEmpty();
                case BEARER -> token().isPresent() && oidcClient().isEmpty();
                case OAUTH2 -> token().isEmpty() && oidcClient().isPresent();
            };
        }
    }

    enum AuthenticationType {
        NONE,
        BEARER,
        OAUTH2
    }

    enum Transport {
        STREAMABLE_HTTP,
        SSE,
        WEBSOCKET
    }

    enum ToolMode {
        ALL,
        INCLUDE,
        EXCLUDE
    }

    interface Tools {

        @WithDefault("all")
        ToolMode mode();

        Optional<List<@NotBlank String>> names();

        Optional<@NotBlankIfPresent String> namePrefix();

        Map<@NotBlank String, SpecificationMapping> specificationMapping();

        @AssertTrue(
                message =
                        "Names are not allowed when mode is ALL, and at least one name must be provided when mode is INCLUDE or EXCLUDE")
        default boolean isNamesValid() {
            if (mode().equals(ToolMode.ALL)) {
                return names().isEmpty();
            }
            return names().isPresent() && !names().get().isEmpty();
        }
    }

    interface SpecificationMapping {

        Optional<@NotBlankIfPresent String> name();

        Optional<@NotBlankIfPresent String> description();
    }
}

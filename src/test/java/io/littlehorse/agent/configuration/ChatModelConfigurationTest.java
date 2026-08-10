package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildExactConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.ConfigValidationException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
class ChatModelConfigurationTest {

    @Inject
    AgentConfiguration agentConfiguration;

    @Test
    void mapsTheSelectedProvider() {
        assertThat(agentConfiguration.chatModel().provider())
                .isEqualTo(ChatModelConfiguration.Provider.ANTHROPIC);
        assertThat(agentConfiguration.chatModel().anthropic()).isPresent();
    }

    @Test
    void requiresAProvider() {
        assertThatThrownBy(() -> buildExactConfiguration(Map.of("agent.task.name", "test-agent")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.chat-model.provider");
    }

    @Test
    void rejectsAnUnknownProvider() {
        assertThatThrownBy(() -> buildExactConfiguration(Map.of(
                        "agent.task.name", "test-agent",
                        "agent.chat-model.provider", "unknown")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void requiresConfigurationForTheSelectedProvider() {
        assertThatThrownBy(() -> buildExactConfiguration(Map.of(
                        "agent.task.name", "test-agent",
                        "agent.chat-model.provider", "anthropic")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "configuration for the selected chat-model provider is missing");
    }
}

package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.ConfigValidationException;

import org.junit.jupiter.api.Test;

import java.util.Map;

class AgentConfigurationTest {

    @Test
    void defaultsMaxToolRoundsToTen() {
        assertThat(buildConfiguration(Map.of()).maxToolRounds()).isEqualTo(10);
    }

    @Test
    void mapsConfiguredMaxToolRounds() {
        assertThat(buildConfiguration(Map.of("agent.max-tool-rounds", "25")).maxToolRounds())
                .isEqualTo(25);
    }

    @Test
    void rejectsNonPositiveMaxToolRounds() {
        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.max-tool-rounds", "0")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.max-tool-rounds")
                .hasMessageContaining("must be greater than zero");

        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.max-tool-rounds", "-1")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.max-tool-rounds")
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void mapsTheCompleteAgentConfigurationTree() {
        AgentConfiguration configuration = buildConfiguration(Map.of(
                "agent.system-message", "You are helpful.",
                "agent.user-message-template", "Process {{struct}}",
                "agent.task.input.type", "STRUCT",
                "agent.task.input.struct.name", "agent-input",
                "agent.mcp.clients.remote.url", "https://mcp.example.test/mcp"));

        assertThat(configuration.systemMessage()).contains("You are helpful.");
        assertThat(configuration.userMessageTemplate()).contains("Process {{struct}}");
        assertThat(configuration.task().name()).isEqualTo("test-agent");
        assertThat(configuration.chatModel().provider())
                .isEqualTo(ChatModelConfiguration.Provider.ANTHROPIC);
        assertThat(configuration.mcp().clients()).containsKey("remote");
    }

    @Test
    void requiresAUserMessageTemplateForStructInput() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.task.input.type",
                        "STRUCT",
                        "agent.task.input.struct.name",
                        "agent-input")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(
                        "user-message-template must be configured when task.input.type is STRUCT");
    }

    @Test
    void rejectsBlankOptionalMessages() {
        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.user-message-template", "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.user-message-template")
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.system-message", "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.system-message")
                .hasMessageContaining("must not be blank");
    }
}

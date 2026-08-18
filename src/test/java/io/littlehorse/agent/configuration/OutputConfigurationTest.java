package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.ConfigValidationException;

import org.junit.jupiter.api.Test;

import java.util.Map;

class OutputConfigurationTest {

    @Test
    void defaultsToTextOutputWithoutStructConfiguration() {
        OutputConfiguration configuration = buildConfiguration(Map.of());

        assertThat(configuration.type()).isEqualTo(OutputConfiguration.Type.TEXT);
        assertThat(configuration.struct()).isEmpty();
    }

    @Test
    void rejectsABlankOutputStructName() {
        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.task.output.struct.name", " \t")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void requiresANameForStructOutput() {
        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.task.output.type", "STRUCT")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("struct must be configured when output type is STRUCT");
    }

    @Test
    void mapsStructOutputConfiguration() {
        OutputConfiguration configuration = buildConfiguration(Map.of(
                "agent.task.output.type", "STRUCT",
                "agent.task.output.struct.name", "agent-result",
                "agent.task.output.struct.version", "7"));

        assertThat(configuration.type()).isEqualTo(OutputConfiguration.Type.STRUCT);
        assertThat(configuration.struct()).isPresent().get().satisfies(struct -> {
            assertThat(struct.name()).isEqualTo("agent-result");
            assertThat(struct.version()).contains(7);
        });
    }

    @Test
    void leavesStructVersionEmptyWhenItIsNotConfigured() {
        OutputConfiguration configuration = buildConfiguration(Map.of(
                "agent.task.output.type",
                "STRUCT",
                "agent.task.output.struct.name",
                "agent-result"));

        assertThat(configuration.struct()).isPresent().get().satisfies(struct -> {
            assertThat(struct.name()).isEqualTo("agent-result");
            assertThat(struct.version()).isEmpty();
        });
    }

    private static OutputConfiguration buildConfiguration(Map<String, String> properties) {
        return AgentConfigurationTestSupport.buildConfiguration(properties)
                .task()
                .output();
    }
}

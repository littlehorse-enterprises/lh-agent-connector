package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.ConfigValidationException;

import org.junit.jupiter.api.Test;

import java.util.Map;

class AgentTaskConfigurationTest {

    @Test
    void mapsTheSharedTaskName() {
        AgentTaskConfiguration configuration = buildConfiguration(Map.of(
                        "agent.task.name",
                        "customer-agent",
                        "agent.task.input.type",
                        "STRUCT",
                        "agent.task.input.struct.name",
                        "customer-request",
                        "agent.user-message-template",
                        "Process {{struct}}",
                        "agent.task.output.type",
                        "TEXT"))
                .task();

        assertThat(configuration.name()).isEqualTo("customer-agent");
        assertThat(configuration.input().type().name()).isEqualTo("STRUCT");
        assertThat(configuration.input().struct()).isPresent();
        assertThat(configuration.output().type().name()).isEqualTo("TEXT");
    }

    @Test
    void rejectsABlankTaskName() {
        assertThatThrownBy(() -> buildConfiguration(Map.of("agent.task.name", " \t")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must not be blank");
    }
}

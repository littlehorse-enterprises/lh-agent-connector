package io.littlehorse.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
@TestProfile(StructToStructAgentTaskSelectionTest.StructToStructProfile.class)
class StructToStructAgentTaskSelectionTest {

    @Inject
    Instance<TextToTextAgentTask> textToTextTask;

    @Inject
    Instance<TextToStructAgentTask> textToStructTask;

    @Inject
    Instance<StructToTextAgentTask> structToTextTask;

    @Inject
    Instance<StructToStructAgentTask> structToStructTask;

    @Test
    void selectsOnlyTheStructToStructTask() {
        assertThat(textToTextTask.isResolvable()).isFalse();
        assertThat(textToStructTask.isResolvable()).isFalse();
        assertThat(structToTextTask.isResolvable()).isFalse();
        assertThat(structToStructTask.isResolvable()).isTrue();
    }

    public static class StructToStructProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.task.input.type",
                    "STRUCT",
                    "agent.task.input.struct.name",
                    "test-agent-input",
                    "agent.task.output.type",
                    "STRUCT",
                    "agent.task.output.struct.name",
                    "test-agent-output",
                    "agent.user-message-template",
                    "Process {{struct}}");
        }
    }
}

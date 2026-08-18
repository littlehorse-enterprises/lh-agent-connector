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
@TestProfile(TextToStructAgentTaskSelectionTest.TextToStructProfile.class)
class TextToStructAgentTaskSelectionTest {

    @Inject
    Instance<TextToTextAgentTask> textToTextTask;

    @Inject
    Instance<TextToStructAgentTask> textToStructTask;

    @Inject
    Instance<StructToTextAgentTask> structToTextTask;

    @Inject
    Instance<StructToStructAgentTask> structToStructTask;

    @Test
    void selectsOnlyTheTextToStructTask() {
        assertThat(textToTextTask.isResolvable()).isFalse();
        assertThat(textToStructTask.isResolvable()).isTrue();
        assertThat(structToTextTask.isResolvable()).isFalse();
        assertThat(structToStructTask.isResolvable()).isFalse();
    }

    public static class TextToStructProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.task.output.type",
                    "STRUCT",
                    "agent.task.output.struct.name",
                    "test-agent-output");
        }
    }
}

package io.littlehorse.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
class TextToTextAgentTaskSelectionTest {

    @Inject
    Instance<TextToTextAgentTask> textToTextTask;

    @Inject
    Instance<TextToStructAgentTask> textToStructTask;

    @Inject
    Instance<StructToTextAgentTask> structToTextTask;

    @Inject
    Instance<StructToStructAgentTask> structToStructTask;

    @Test
    void selectsOnlyTheTextToTextTask() {
        assertThat(textToTextTask.isResolvable()).isTrue();
        assertThat(textToStructTask.isResolvable()).isFalse();
        assertThat(structToTextTask.isResolvable()).isFalse();
        assertThat(structToStructTask.isResolvable()).isFalse();
    }
}

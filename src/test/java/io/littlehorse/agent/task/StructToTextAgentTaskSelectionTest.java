package io.littlehorse.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.OutputConfiguration;
import io.littlehorse.agent.execution.AgentExecutor;
import io.littlehorse.agent.mcp.ToolsManager;
import io.littlehorse.agent.message.UserMessageTemplateRenderer;
import io.littlehorse.agent.structuredoutput.StructDefResolver;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

@QuarkusTest
@TestProfile(StructToTextAgentTaskSelectionTest.TextOutputProfile.class)
class StructToTextAgentTaskSelectionTest {

    @Inject
    AgentExecutor executor;

    @Inject
    Instance<TextToTextAgentTask> textToTextTask;

    @Inject
    Instance<TextToStructAgentTask> textToStructTask;

    @Inject
    Instance<StructToTextAgentTask> structToTextTask;

    @Inject
    Instance<StructToStructAgentTask> structToStructTask;

    @Inject
    ChatModel chatModel;

    @Inject
    Optional<SystemMessage> systemMessage;

    @Inject
    McpToolProvider mcpToolProvider;

    @Inject
    ToolsManager toolsManager;

    @Inject
    Instance<StructDefResolver> structDefResolver;

    @Inject
    AgentConfiguration agentConfiguration;

    @Inject
    UserMessageTemplateRenderer userMessageTemplateRenderer;

    @Test
    void selectsOnlyTheStructToTextTaskAndInjectsItsRuntimeDependencies() {
        assertThat(executor).isNotNull();
        assertThat(textToTextTask.isResolvable()).isFalse();
        assertThat(textToStructTask.isResolvable()).isFalse();
        assertThat(structToTextTask.isResolvable()).isTrue();
        assertThat(structToStructTask.isResolvable()).isFalse();
        assertThat(chatModel).isInstanceOf(AnthropicChatModel.class);
        assertThat(systemMessage).contains(SystemMessage.from("You are a test assistant."));
        assertThat(mcpToolProvider).isNotNull();
        assertThat(toolsManager).isNotNull();
        assertThat(agentConfiguration.task().output().type())
                .isEqualTo(OutputConfiguration.Type.TEXT);
        assertThat(structDefResolver.isResolvable()).isFalse();
        assertThat(userMessageTemplateRenderer).isNotNull();
    }

    public static class TextOutputProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.task.input.type",
                    "STRUCT",
                    "agent.task.input.struct.name",
                    "test-agent-input",
                    "agent.user-message-template",
                    "Process {{struct}}");
        }
    }
}

package io.littlehorse.agent.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
class AnthropicChatModelConfigurationTest {

    @Inject
    AgentConfiguration configuration;

    @Inject
    ChatModel chatModel;

    @Test
    void mapsAndSelectsTheAnthropicConfigurationAtStartup() {
        AnthropicChatModelConfiguration anthropic =
                configuration.chatModel().anthropic().orElseThrow();
        assertThat(anthropic.apiKey()).isEqualTo("test-api-key");
        assertThat(anthropic.model()).isEqualTo("claude-3");
        assertThat(anthropic.baseUrl()).isEmpty();
        assertThat(chatModel).isInstanceOf(AnthropicChatModel.class);
    }
}

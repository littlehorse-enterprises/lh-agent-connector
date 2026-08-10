package io.littlehorse.agent.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
@TestProfile(OpenAiChatModelConfigurationTest.OpenAiProfile.class)
class OpenAiChatModelConfigurationTest {

    @Inject
    AgentConfiguration configuration;

    @Inject
    ChatModel chatModel;

    @Test
    void mapsAndSelectsTheOfficialOpenAiConfigurationAtStartup() {
        OpenAiChatModelConfiguration openAi = configuration.chatModel().openai().orElseThrow();
        assertThat(openAi.apiKey()).isEqualTo("test-openai-api-key");
        assertThat(openAi.baseUrl()).contains("https://openai.example.test/v1");
        assertThat(openAi.model()).isEqualTo("test-openai-model");
        assertThat(chatModel).isInstanceOf(OpenAiChatModel.class);
    }

    public static class OpenAiProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.chat-model.provider", "openai",
                    "agent.chat-model.openai.api-key", "test-openai-api-key",
                    "agent.chat-model.openai.base-url", "https://openai.example.test/v1",
                    "agent.chat-model.openai.model", "test-openai-model");
        }
    }
}

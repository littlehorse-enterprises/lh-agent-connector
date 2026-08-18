package io.littlehorse.agent.configuration;

import static io.littlehorse.agent.configuration.AgentConfigurationTestSupport.buildConfiguration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.ConfigValidationException;

import org.junit.jupiter.api.Test;

import java.util.Map;

class OpenAiChatModelConfigurationValidationTest {

    @Test
    void requiresApiKeyAndModel() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.model",
                        "test-model")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.chat-model.openai.api-key");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.api-key",
                        "test-api-key")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.chat-model.openai.model");
    }

    @Test
    void acceptsRequiredConfiguration() {
        assertThatCode(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.api-key",
                        "test-api-key",
                        "agent.chat-model.openai.model",
                        "test-model")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankRequiredConfiguration() {
        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.api-key",
                        "   ",
                        "agent.chat-model.openai.model",
                        "test-model")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be configured");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.api-key",
                        "test-api-key",
                        "agent.chat-model.openai.model",
                        "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be configured");

        assertThatThrownBy(() -> buildConfiguration(Map.of(
                        "agent.chat-model.provider",
                        "openai",
                        "agent.chat-model.openai.api-key",
                        "test-api-key",
                        "agent.chat-model.openai.model",
                        "test-model",
                        "agent.chat-model.openai.base-url",
                        "   ")))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("agent.chatModel.openai.base-url");
    }
}

package io.littlehorse.agent.configuration;

import jakarta.validation.constraints.AssertTrue;

import java.util.Optional;

/** Selects the single chat-model provider used by the agent. */
public interface ChatModelConfiguration {

    Provider provider();

    Optional<AnthropicChatModelConfiguration> anthropic();

    Optional<OpenAiChatModelConfiguration> openai();

    @AssertTrue(message = "configuration for the selected chat-model provider is missing")
    default boolean isProviderConfigurationValid() {
        return switch (provider()) {
            case ANTHROPIC -> anthropic().isPresent();
            case OPENAI -> openai().isPresent();
        };
    }

    enum Provider {
        ANTHROPIC,
        OPENAI
    }
}

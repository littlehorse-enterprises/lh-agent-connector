package io.littlehorse.agent.chat;

import dev.langchain4j.model.anthropic.AnthropicChatModel;

import io.littlehorse.agent.configuration.AnthropicChatModelConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;

@ApplicationScoped
public class AnthropicChatModelFactory {

    public AnthropicChatModel create(AnthropicChatModelConfiguration configuration) {
        return AnthropicChatModel.builder()
                .apiKey(configuration.apiKey())
                .modelName(configuration.model())
                .version(configuration.version().orElse("2023-06-01"))
                .timeout(configuration.timeout().orElse(Duration.ofSeconds(10)))
                .temperature(configuration.temperature().orElse(null))
                .maxTokens(configuration.maxTokens().orElse(1024))
                .topP(configuration.topP().orElse(null))
                .topK(configuration.topK().orElse(null))
                .maxRetries(configuration.maxRetries().orElse(2))
                .stopSequences(configuration.stopSequences().orElse(null))
                .logRequests(configuration.logRequests().orElse(false))
                .logResponses(configuration.logResponses().orElse(false))
                .cacheSystemMessages(configuration.cacheSystemMessages().orElse(false))
                .cacheTools(configuration.cacheTools().orElse(false))
                .baseUrl(configuration
                        .baseUrl()
                        .filter(value -> !value.isBlank())
                        .orElse(null))
                .build();
    }
}

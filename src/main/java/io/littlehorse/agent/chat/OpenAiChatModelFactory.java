package io.littlehorse.agent.chat;

import dev.langchain4j.model.openai.OpenAiChatModel;

import io.littlehorse.agent.configuration.OpenAiChatModelConfiguration;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OpenAiChatModelFactory {

    public OpenAiChatModel create(OpenAiChatModelConfiguration configuration) {
        return OpenAiChatModel.builder()
                .apiKey(configuration.apiKey())
                .modelName(configuration.model())
                .timeout(configuration.timeout().orElse(null))
                .maxRetries(configuration.maxRetries().orElse(2))
                .organizationId(configuration.organizationId().orElse(null))
                .projectId(configuration.projectId().orElse(null))
                .temperature(configuration.temperature().orElse(null))
                .topP(configuration.topP().orElse(null))
                .maxTokens(configuration.maxTokens().orElse(null))
                .maxCompletionTokens(configuration.maxCompletionTokens().orElse(null))
                .presencePenalty(configuration.presencePenalty().orElse(null))
                .frequencyPenalty(configuration.frequencyPenalty().orElse(null))
                .logRequests(configuration.logRequests().orElse(false))
                .logResponses(configuration.logResponses().orElse(false))
                .stop(configuration.stop().orElse(null))
                .reasoningEffort(configuration.reasoningEffort().orElse(null))
                .serviceTier(configuration.serviceTier().orElse(null))
                .baseUrl(configuration
                        .baseUrl()
                        .filter(value -> !value.isBlank())
                        .orElse(null))
                .build();
    }
}

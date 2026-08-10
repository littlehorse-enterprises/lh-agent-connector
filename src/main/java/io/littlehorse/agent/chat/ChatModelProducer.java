package io.littlehorse.agent.chat;

import dev.langchain4j.model.chat.ChatModel;

import io.littlehorse.agent.configuration.AgentConfiguration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

@ApplicationScoped
public class ChatModelProducer {

    private final OpenAiChatModelFactory openAiFactory;
    private final AnthropicChatModelFactory anthropicFactory;

    public ChatModelProducer(
            OpenAiChatModelFactory openAiFactory, AnthropicChatModelFactory anthropicFactory) {
        this.openAiFactory = openAiFactory;
        this.anthropicFactory = anthropicFactory;
    }

    @Singleton
    public ChatModel chatModel(AgentConfiguration agentConfiguration) {
        var configuration = agentConfiguration.chatModel();
        return switch (configuration.provider()) {
            case OPENAI -> openAiFactory.create(configuration.openai().orElseThrow());
            case ANTHROPIC -> anthropicFactory.create(configuration.anthropic().orElseThrow());
        };
    }
}

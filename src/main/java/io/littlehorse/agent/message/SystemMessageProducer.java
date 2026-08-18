package io.littlehorse.agent.message;

import dev.langchain4j.data.message.SystemMessage;

import io.littlehorse.agent.configuration.AgentConfiguration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import java.util.Optional;

@ApplicationScoped
public class SystemMessageProducer {

    @Singleton
    public Optional<SystemMessage> systemMessage(AgentConfiguration configuration) {
        return configuration.systemMessage().map(SystemMessage::from);
    }
}

package io.littlehorse.agent.configuration;

import io.littlehorse.agent.configuration.validation.NotBlankIfPresent;
import io.smallrye.config.ConfigMapping;

import jakarta.validation.constraints.AssertTrue;

import java.util.Optional;

/** Application configuration for the agent. */
@ConfigMapping(prefix = "agent")
public interface AgentConfiguration {

    Optional<@NotBlankIfPresent String> userMessageTemplate();

    Optional<@NotBlankIfPresent String> systemMessage();

    AgentTaskConfiguration task();

    ChatModelConfiguration chatModel();

    McpConfiguration mcp();

    @AssertTrue(message = "user-message-template must be configured when task.input.type is STRUCT")
    default boolean isUserMessageTemplateValid() {
        return task().input().type() != InputConfiguration.Type.STRUCT
                || userMessageTemplate().isPresent();
    }
}

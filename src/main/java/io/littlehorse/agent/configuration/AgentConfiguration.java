package io.littlehorse.agent.configuration;

import io.littlehorse.agent.configuration.validation.NotBlankIfPresent;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

import java.util.Optional;

/** Application configuration for the agent. */
@ConfigMapping(prefix = "agent")
public interface AgentConfiguration {

    @WithDefault("10")
    @Min(value = 1, message = "must be greater than zero")
    int maxToolRounds();

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

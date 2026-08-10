package io.littlehorse.agent.configuration;

import jakarta.validation.constraints.NotBlank;

public interface AgentTaskConfiguration {

    @NotBlank
    String name();

    InputConfiguration input();

    OutputConfiguration output();
}

package io.littlehorse.agent.configuration;

import io.smallrye.config.WithDefault;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public interface OutputConfiguration {

    @WithDefault("TEXT")
    Type type();

    Optional<StructConfiguration> struct();

    @AssertTrue(message = "struct must be configured when output type is STRUCT")
    default boolean isStructConfigurationValid() {
        return type() != Type.STRUCT || struct().isPresent();
    }

    enum Type {
        TEXT,
        STRUCT
    }

    interface StructConfiguration {

        @NotBlank
        String name();

        Optional<@Min(value = -1, message = "must be -1 or greater") Integer> version();
    }
}

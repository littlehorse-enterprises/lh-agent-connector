package io.littlehorse.agent.message;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

/** Ensures the conditional Struct-input template is compiled during application startup. */
@ApplicationScoped
public class UserMessageTemplateInitializer {

    void initialize(
            @Observes StartupEvent ignored,
            Instance<UserMessageTemplateRenderer> userMessageTemplateRenderer) {
        if (userMessageTemplateRenderer.isResolvable()) {
            userMessageTemplateRenderer.get();
        }
    }
}

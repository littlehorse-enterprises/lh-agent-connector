package io.littlehorse.connector.general;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import io.quarkiverse.langchain4j.RegisterAiService;

import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
/* CAUTION: this agent has memory, so it will remember previous interactions */
public interface GeneralPurposesLLM {

    // The system message is supplied at call time (sourced from configuration) so the persona can be
    // changed without recompiling.
    @SystemMessage("{systemMessage}")
    @UserMessage("{prompt}")
    String answer(@MemoryId String memoryId, @V("systemMessage") String systemMessage, @V("prompt") String prompt);
}

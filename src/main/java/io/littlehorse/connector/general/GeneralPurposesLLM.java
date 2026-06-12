package io.littlehorse.connector.general;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
/* CAUTION: this agent has memory, so it will remember previous interactions */
public interface GeneralPurposesLLM {

    @SystemMessage("You are a helpful assistant. Answer concisely and accurately.")
    @UserMessage("{prompt}")
    String answer(@MemoryId String memoryId, String prompt);
}

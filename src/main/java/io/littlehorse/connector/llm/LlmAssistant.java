package io.littlehorse.connector.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface LlmAssistant {

    @SystemMessage("You are a helpful assistant. Answer concisely and accurately.")
    @UserMessage("{prompt}")
    String answer(String prompt);
}

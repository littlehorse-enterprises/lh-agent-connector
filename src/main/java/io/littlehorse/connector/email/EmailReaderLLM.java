package io.littlehorse.connector.email;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import io.quarkiverse.langchain4j.RegisterAiService;

import jakarta.enterprise.context.ApplicationScoped;

/** Stateless (no memory) AI service that classifies the content of an email. */
@RegisterAiService(
        chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
@ApplicationScoped
public interface EmailReaderLLM {

    @SystemMessage("""
            You are an email classifier. Read the email and classify it into exactly one type,
            returning a JSON object with two fields: `type` and `subject`.

            Rules:
            - If the email is spam, set `type` to SPAM and `subject` to a short explanation of the
              possible cause, e.g. "Possible spam because <cause>".
            - If the email is a job opportunity, set `type` to JOB_OPPORTUNITY and `subject` to a
              summary that includes the company and any other relevant information.
            - Otherwise set `type` to NOT_IMPORTANT and `subject` to the subject of the email.
            """)
    @UserMessage("{email}")
    EmailClassification classify(String email);
}

package io.littlehorse.connector.support;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import io.quarkiverse.langchain4j.RegisterAiService;

import jakarta.enterprise.context.ApplicationScoped;

/** Stateless (no memory) AI service that triages support tickets and drafts customer replies. */
@RegisterAiService(
        chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
@ApplicationScoped
public interface SupportLLM {

    @SystemMessage("""
            You triage customer support tickets written in natural language. Classify the ticket
            into exactly one type and return a JSON object with two fields: `type` and `summary`.

            Rules:
            - If the customer just shares an opinion, praise or complaint that needs no action, set
              `type` to FEEDBACK and `summary` to a one-line summary of the feedback.
            - If the customer reports a problem or asks for help that a human agent must act on, set
              `type` to SUPPORT_REQUEST and `summary` to a concise description of what they need.
            """)
    @UserMessage("{ticket}")
    SupportClassification classify(String ticket);

    @SystemMessage("""
            You are a friendly customer support agent. Using the original ticket and the resolution
            notes provided by a human agent, write a short, polite reply addressed to the customer.
            Only output the reply text.
            """)
    @UserMessage("""
            Original ticket:
            {ticket}

            Human agent resolution notes:
            {resolution}
            """)
    String draftReply(@V("ticket") String ticket, @V("resolution") String resolution);
}

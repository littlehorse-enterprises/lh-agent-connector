package io.littlehorse.connector.support;

import dev.langchain4j.exception.NonRetriableException;

import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.exception.LHTaskException;
import io.littlehorse.sdk.worker.LHTaskMethod;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class SupportTask {

    private static final Logger LOG = LoggerFactory.getLogger(SupportTask.class);

    private final SupportLLM assistant;

    @Inject
    public SupportTask(final SupportLLM assistant) {
        this.assistant = assistant;
    }

    @LHTaskMethod(
            value = "classify-support-ticket",
            description = "Classifies a support ticket as FEEDBACK or SUPPORT_REQUEST.")
    public SupportClassification classifyTicket(final String ticket) {
        LOG.info("Classifying support ticket ({} chars)", ticket.length());
        try {
            return assistant.classify(ticket);
        } catch (final NonRetriableException e) {
            LOG.error("Non-retriable LLM error while classifying support ticket", e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        }
    }

    @LHTaskMethod(
            value = "log-feedback",
            description = "Logs that the ticket is plain customer feedback (no action needed).")
    public void logFeedback(final String summary) {
        LOG.info("Received customer feedback: {}", summary);
    }

    @LHTaskMethod(
            value = "draft-support-reply",
            description = "Drafts a customer reply from the human agent's resolution notes.")
    public String draftReply(final String ticket, final String resolution) {
        LOG.info("Drafting support reply from human resolution notes");
        try {
            return assistant.draftReply(ticket, resolution);
        } catch (final NonRetriableException e) {
            LOG.error("Non-retriable LLM error while drafting support reply", e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        }
    }
}

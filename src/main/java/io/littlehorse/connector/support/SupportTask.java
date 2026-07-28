package io.littlehorse.connector.support;

import io.littlehorse.connector.logging.LlmContext;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;

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
    public SupportClassification classifyTicket(final String ticket, final WorkerContext context) {
        LOG.info("Classifying support ticket ({} chars)", ticket.length());
        return LlmContext.runLlmTask(context, () -> assistant.classify(ticket));
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
    public String draftReply(
            final String ticket, final String resolution, final WorkerContext context) {
        LOG.info("Drafting support reply from human resolution notes");
        return LlmContext.runLlmTask(context, () -> assistant.draftReply(ticket, resolution));
    }
}

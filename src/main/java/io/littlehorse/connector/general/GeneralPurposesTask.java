package io.littlehorse.connector.general;

import dev.langchain4j.exception.NonRetriableException;

import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.exception.LHTaskException;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class GeneralPurposesTask {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralPurposesTask.class);

    private final GeneralPurposesLLM assistant;

    @Inject
    public GeneralPurposesTask(final GeneralPurposesLLM assistant) {
        this.assistant = assistant;
    }

    @LHTaskMethod(
            value = "ask-llm",
            description = "Sends a prompt to the LLM and returns its response.")
    public String askLlm(final String prompt, final WorkerContext context) {
        // Use the WfRunId as the memory id so each WfRun keeps its own conversation context.
        final String memoryId = LHLibUtil.wfRunIdToString(context.getWfRunId());
        LOG.info("Received prompt for LLM (wfRunId={}): {}", memoryId, prompt);
        try {
            return assistant.answer(memoryId, prompt);
        } catch (final NonRetriableException e) {
            // Permanent failures (auth/empty credits, misconfiguration, invalid request): do not
            // retry. Throwing LHTaskException raises a business EXCEPTION instead of a retryable
            // TASK_FAILURE.
            LOG.error("Non-retriable LLM error while answering prompt", e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        }
        // RetriableException (timeouts, rate limits, server errors) and any other RuntimeException
        // propagate as a retryable TASK_FAILURE so LittleHorse retries the task.
    }
}

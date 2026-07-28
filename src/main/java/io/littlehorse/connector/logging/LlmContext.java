package io.littlehorse.connector.logging;

import dev.langchain4j.exception.NonRetriableException;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.exception.LHTaskException;
import io.littlehorse.sdk.worker.WorkerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Runs an LLM call from a task while (1) binding the current LittleHorse {@code TaskRunId} to the
 * thread so the {@link dev.langchain4j.model.chat.listener.ChatModelListener} can correlate the
 * logged LLM interaction with the task run that triggered it, and (2) translating a langchain4j
 * {@code NonRetriableException} into a non-retryable {@link LHTaskException} business error.
 *
 * <p>The langchain4j model invocation is synchronous on the task worker thread, so a thread-local
 * set right before the assistant call is visible to the listener. Because worker threads are pooled
 * and reused, the value is always cleared afterwards (in a {@code finally}), so no task can leak a
 * stale id onto the next TaskRun.
 */
public final class LlmContext {

    private static final Logger LOG = LoggerFactory.getLogger(LlmContext.class);

    private static final ThreadLocal<String> CURRENT_TASK_RUN_ID = new ThreadLocal<>();

    private LlmContext() {}

    /**
     * Runs {@code action} with the TaskRunId of {@code context} bound to the current thread. A
     * {@code NonRetriableException} is logged and rethrown as a non-retryable {@link
     * LHTaskException}; any other exception propagates unchanged so LittleHorse retries the task.
     * The bound id is always cleared afterwards, even if the action throws.
     */
    public static <T> T runLlmTask(final WorkerContext context, final Supplier<T> action) {
        CURRENT_TASK_RUN_ID.set(LHLibUtil.taskRunIdToString(context.getTaskRunId()));
        try {
            return action.get();
        } catch (final NonRetriableException e) {
            // Permanent failures (auth/empty credits, misconfiguration, invalid request): do not
            // retry. LHTaskException raises a business EXCEPTION instead of a retryable TASK_FAILURE.
            LOG.error("Non-retriable LLM error: {}", e.getMessage(), e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        } finally {
            CURRENT_TASK_RUN_ID.remove();
        }
    }

    public static String currentTaskRunId() {
        return CURRENT_TASK_RUN_ID.get();
    }
}

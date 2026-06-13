package io.littlehorse.connector.filesystem;

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
public class FilesystemAgentTask {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemAgentTask.class);

    private final FilesystemAgentLLM agent;

    @Inject
    public FilesystemAgentTask(final FilesystemAgentLLM agent) {
        this.agent = agent;
    }

    @LHTaskMethod(
            value = "run-filesystem-agent",
            description =
                    "Runs one turn of the filesystem agent; it either finishes or asks the human.")
    public AgentResponse work(final String message, final WorkerContext context) {
        // Use the WfRunId as the memory id so the agent keeps the conversation across turns.
        final String memoryId = LHLibUtil.wfRunIdToString(context.getWfRunId());
        LOG.info("Running filesystem agent turn (wfRunId={})", memoryId);
        try {
            final AgentResponse response = agent.work(memoryId, message);
            LOG.info("Agent turn finished with status {}", response.status());
            return response;
        } catch (final NonRetriableException e) {
            LOG.error("Non-retriable LLM error while running the filesystem agent", e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        }
    }
}

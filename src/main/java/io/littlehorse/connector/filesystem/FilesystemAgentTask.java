package io.littlehorse.connector.filesystem;

import io.littlehorse.connector.logging.LlmContext;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class FilesystemAgentTask {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemAgentTask.class);

    private final FilesystemAgentLLM agent;
    private final String workspace;

    @Inject
    public FilesystemAgentTask(
            final FilesystemAgentLLM agent,
            @ConfigProperty(name = "lhc.agent.workspace") final String workspace) {
        this.agent = agent;
        this.workspace = workspace;
    }

    @LHTaskMethod(
            value = "run-filesystem-agent",
            description =
                    "Runs one turn of the filesystem agent; it either finishes or asks the human.")
    public AgentResponse work(final String message, final WorkerContext context) {
        // Use the WfRunId as the memory id so the agent keeps the conversation across turns.
        final String memoryId = LHLibUtil.wfRunIdToString(context.getWfRunId());
        LOG.info("Running filesystem agent turn (wfRunId={})", memoryId);
        final AgentResponse response =
                LlmContext.runLlmTask(context, () -> agent.work(memoryId, workspace, message));
        LOG.info("Agent turn finished with status {}", response.status());
        return response;
    }
}

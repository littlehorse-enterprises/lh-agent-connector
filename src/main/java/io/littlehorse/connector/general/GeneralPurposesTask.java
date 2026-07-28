package io.littlehorse.connector.general;

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
public class GeneralPurposesTask {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralPurposesTask.class);

    private final GeneralPurposesLLM assistant;
    private final String systemMessage;

    @Inject
    public GeneralPurposesTask(
            final GeneralPurposesLLM assistant,
            @ConfigProperty(name = "lhc.general.system-message") final String systemMessage) {
        this.assistant = assistant;
        this.systemMessage = systemMessage;
    }

    @LHTaskMethod(
            value = "ask-llm",
            description = "Sends a prompt to the LLM and returns its response.")
    public String askLlm(final String prompt, final WorkerContext context) {
        // Use the WfRunId as the memory id so each WfRun keeps its own conversation context.
        final String memoryId = LHLibUtil.wfRunIdToString(context.getWfRunId());
        LOG.info("Received prompt for LLM (wfRunId={}): {}", memoryId, prompt);
        return LlmContext.runLlmTask(context, () -> assistant.answer(memoryId, systemMessage, prompt));
    }

    @LHTaskMethod(
            value = "print-topic",
            description = "Asks the LLM what the current session is about and prints it.")
    public String printTopic(final WorkerContext context) {
        // Use the WfRunId as the memory id so the question is answered within this WfRun's context.
        final String memoryId = LHLibUtil.wfRunIdToString(context.getWfRunId());
        final String topic = LlmContext.runLlmTask(
                context,
                () -> assistant.answer(
                        memoryId, systemMessage, "What are we talking about in this session?"));
        LOG.info("Current topic (wfRunId={}): {}", memoryId, topic);
        return topic;
    }
}

package io.littlehorse.connector.task;

import io.littlehorse.connector.llm.LlmAssistant;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.worker.LHTaskMethod;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@LHTask
public class LlmTask {

    private static final Logger LOG = Logger.getLogger(LlmTask.class);

    private final LlmAssistant assistant;

    @Inject
    public LlmTask(LlmAssistant assistant) {
        this.assistant = assistant;
    }

    @LHTaskMethod(value = "ask-llm", description = "Sends a prompt to the LLM and returns its response.")
    public String askLlm(String prompt) {
        LOG.infof("Received prompt for LLM: %s", prompt);
        return assistant.answer(prompt);
    }
}

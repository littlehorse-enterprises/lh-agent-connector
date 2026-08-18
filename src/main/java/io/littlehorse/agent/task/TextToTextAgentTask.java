package io.littlehorse.agent.task;

import io.littlehorse.agent.execution.AgentExecutor;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;
import io.quarkus.arc.lookup.LookupIfProperty;

@LHTask
@LookupIfProperty(name = "agent.task.input.type", stringValue = "TEXT", lookupIfMissing = true)
@LookupIfProperty(name = "agent.task.output.type", stringValue = "TEXT", lookupIfMissing = true)
public class TextToTextAgentTask {

    private final AgentExecutor executor;

    public TextToTextAgentTask(AgentExecutor executor) {
        this.executor = executor;
    }

    @LHTaskMethod(
            value = "${agent.task.name}",
            description = "Sends one text user message to an LLM and returns a text response.")
    public String execute(String userMessage, WorkerContext context) {
        return executor.textToText(userMessage, context);
    }
}

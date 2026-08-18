package io.littlehorse.agent.task;

import io.littlehorse.agent.execution.AgentExecutor;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHType;
import io.littlehorse.sdk.worker.WorkerContext;
import io.quarkus.arc.lookup.LookupIfProperty;

@LHTask
@LookupIfProperty(name = "agent.task.input.type", stringValue = "TEXT", lookupIfMissing = true)
@LookupIfProperty(name = "agent.task.output.type", stringValue = "STRUCT")
public class TextToStructAgentTask {

    private final AgentExecutor executor;

    public TextToStructAgentTask(AgentExecutor executor) {
        this.executor = executor;
    }

    @LHTaskMethod(
            value = "${agent.task.name}",
            description = "Sends one text user message to an LLM and returns a Struct response.")
    @LHType(structDefName = "${agent.task.output.struct.name}")
    public InlineStruct execute(String userMessage, WorkerContext context) {
        return executor.textToStruct(userMessage, context);
    }
}

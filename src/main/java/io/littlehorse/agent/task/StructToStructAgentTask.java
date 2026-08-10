package io.littlehorse.agent.task;

import io.littlehorse.agent.execution.AgentExecutor;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHType;
import io.littlehorse.sdk.worker.WorkerContext;
import io.quarkus.arc.lookup.LookupIfProperty;

@LHTask
@LookupIfProperty(name = "agent.task.input.type", stringValue = "STRUCT")
@LookupIfProperty(name = "agent.task.output.type", stringValue = "STRUCT")
public class StructToStructAgentTask {

    private final AgentExecutor executor;

    public StructToStructAgentTask(AgentExecutor executor) {
        this.executor = executor;
    }

    @LHTaskMethod(
            value = "${agent.task.name}",
            description =
                    "Renders a LittleHorse Struct into a user message, sends it to an LLM, and returns a Struct response.")
    @LHType(structDefName = "${agent.task.output.struct.name}")
    public InlineStruct execute(
            @LHType(structDefName = "${agent.task.input.struct.name}") InlineStruct input,
            WorkerContext context) {
        return executor.structToStruct(input, context);
    }
}

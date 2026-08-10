package io.littlehorse.agent.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Objects;

/** LittleHorse Struct representation of a LangChain4j {@link ToolExecutionRequest}. */
@LHStructDef(
        value = "tool-execution-request",
        description = "A LangChain4j request to execute a tool.")
public class ToolExecutionRequestStruct {

    @LHStructField(isNullable = true)
    private String id;

    @LHStructField(isNullable = true)
    private String name;

    @LHStructField(isNullable = true)
    private String arguments;

    public ToolExecutionRequestStruct() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public static ToolExecutionRequestStruct from(ToolExecutionRequest request) {
        Objects.requireNonNull(request);
        ToolExecutionRequestStruct struct = new ToolExecutionRequestStruct();
        struct.setId(request.id());
        struct.setName(request.name());
        struct.setArguments(request.arguments());
        return struct;
    }

    public ToolExecutionRequest toToolExecutionRequest() {
        return ToolExecutionRequest.builder()
                .id(this.getId())
                .name(this.getName())
                .arguments(this.getArguments())
                .build();
    }
}

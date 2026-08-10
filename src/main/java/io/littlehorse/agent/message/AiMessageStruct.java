package io.littlehorse.agent.message;

import dev.langchain4j.data.message.AiMessage;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Objects;

/**
 * Explicit LittleHorse Struct representation of a LangChain4j {@link AiMessage}.
 */
@LHStructDef(value = "ai-message", description = "A LangChain4j AI message.")
public class AiMessageStruct {

    @LHStructField(isNullable = true)
    private String text;

    @LHStructField(isNullable = true)
    private String thinking;

    private ToolExecutionRequestStruct[] toolExecutionRequests = new ToolExecutionRequestStruct[0];

    public AiMessageStruct() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getThinking() {
        return thinking;
    }

    public void setThinking(String thinking) {
        this.thinking = thinking;
    }

    public ToolExecutionRequestStruct[] getToolExecutionRequests() {
        return toolExecutionRequests;
    }

    public void setToolExecutionRequests(ToolExecutionRequestStruct[] toolExecutionRequests) {
        this.toolExecutionRequests = Objects.requireNonNull(toolExecutionRequests);
    }

    public static AiMessageStruct from(AiMessage aiMessage) {
        Objects.requireNonNull(aiMessage);
        AiMessageStruct struct = new AiMessageStruct();
        struct.setText(aiMessage.text());
        struct.setThinking(aiMessage.thinking());
        struct.setToolExecutionRequests(aiMessage.toolExecutionRequests().stream()
                .map(ToolExecutionRequestStruct::from)
                .toArray(ToolExecutionRequestStruct[]::new));
        return struct;
    }
}

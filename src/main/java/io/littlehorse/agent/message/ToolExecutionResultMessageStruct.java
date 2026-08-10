package io.littlehorse.agent.message;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Arrays;
import java.util.Objects;

/** Explicit LittleHorse Struct representation of {@link ToolExecutionResultMessage}. */
@LHStructDef(
        value = "tool-execution-result-message",
        description = "The result of a LangChain4j tool execution.")
public class ToolExecutionResultMessageStruct implements ChatMessageConverter {

    @LHStructField(isNullable = true)
    private String id;

    @LHStructField(isNullable = true)
    private String toolName;

    private TextContentStruct[] contents = new TextContentStruct[0];

    @LHStructField(isNullable = true)
    private Boolean isError;

    public ToolExecutionResultMessageStruct() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public TextContentStruct[] getContents() {
        return contents;
    }

    public void setContents(TextContentStruct[] contents) {
        this.contents = Objects.requireNonNull(contents);
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }

    public static ToolExecutionResultMessageStruct from(ToolExecutionResultMessage message) {
        Objects.requireNonNull(message);
        ToolExecutionResultMessageStruct struct = new ToolExecutionResultMessageStruct();
        struct.setId(message.id());
        struct.setToolName(message.toolName());
        struct.setContents(message.contents().stream()
                .map(ToolExecutionResultMessageStruct::toTextContentStruct)
                .toArray(TextContentStruct[]::new));
        struct.setIsError(message.isError());
        return struct;
    }

    @Override
    public ChatMessage toChatMessage() {
        TextContent[] contents = Arrays.stream(this.getContents())
                .map(TextContentStruct::toTextContent)
                .toArray(TextContent[]::new);
        return ToolExecutionResultMessage.builder()
                .id(this.getId())
                .toolName(this.getToolName())
                .contents(contents)
                .isError(this.getIsError())
                .build();
    }

    private static TextContentStruct toTextContentStruct(Content content) {
        if (content instanceof TextContent textContent) {
            return TextContentStruct.from(textContent);
        }
        throw new IllegalArgumentException(
                "Only TextContent is supported in tool execution result contents");
    }
}

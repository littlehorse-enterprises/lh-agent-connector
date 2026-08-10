package io.littlehorse.agent.message;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Arrays;
import java.util.Objects;

/**
 * Explicit LittleHorse Struct representation of {@link UserMessage}.
 */
@LHStructDef(value = "user-message", description = "A LangChain4j user message.")
public class UserMessageStruct implements ChatMessageConverter {

    @LHStructField(isNullable = true)
    private String name;

    private TextContentStruct[] contents = new TextContentStruct[0];

    public UserMessageStruct() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TextContentStruct[] getContents() {
        return contents;
    }

    public void setContents(TextContentStruct[] contents) {
        this.contents = Objects.requireNonNull(contents);
    }

    public static UserMessageStruct from(UserMessage message) {
        Objects.requireNonNull(message);
        UserMessageStruct struct = new UserMessageStruct();
        struct.setName(message.name());
        struct.setContents(message.contents().stream()
                .map(UserMessageStruct::toTextContentStruct)
                .toArray(TextContentStruct[]::new));
        return struct;
    }

    @Override
    public ChatMessage toChatMessage() {
        return UserMessage.builder()
                .name(this.getName())
                .contents(Arrays.stream(this.getContents())
                        .map(TextContentStruct::toTextContent)
                        .map(Content.class::cast)
                        .toList())
                .build();
    }

    private static TextContentStruct toTextContentStruct(Content content) {
        if (content instanceof TextContent textContent) {
            return TextContentStruct.from(textContent);
        }
        throw new IllegalArgumentException(
                "Only TextContent is supported in user message contents");
    }
}

package io.littlehorse.agent.message;

import dev.langchain4j.data.message.TextContent;

import io.littlehorse.sdk.worker.LHStructDef;

import java.util.Objects;

/** LittleHorse Struct representation of LangChain4j {@link TextContent}. */
@LHStructDef(value = "text-content", description = "Text content in a chat message.")
public class TextContentStruct {

    private String text;

    public TextContentStruct() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text);
    }

    public static TextContentStruct from(TextContent content) {
        Objects.requireNonNull(content);
        TextContentStruct struct = new TextContentStruct();
        struct.setText(content.text());
        return struct;
    }

    public static TextContent toTextContent(TextContentStruct struct) {
        Objects.requireNonNull(struct);
        return TextContent.from(struct.getText());
    }
}

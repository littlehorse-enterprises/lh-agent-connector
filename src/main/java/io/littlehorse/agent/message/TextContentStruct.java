package io.littlehorse.agent.message;

import dev.langchain4j.data.message.TextContent;

import java.util.Objects;

/** LittleHorse Struct representation of LangChain4j {@link TextContent}. */
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

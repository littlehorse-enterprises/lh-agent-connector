package io.littlehorse.agent.message;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

import io.littlehorse.sdk.worker.LHStructDef;

import java.util.Objects;

/** Explicit LittleHorse Struct representation of {@link SystemMessage}. */
@LHStructDef(value = "system-message", description = "A LangChain4j system message.")
public class SystemMessageStruct implements ChatMessageConverter {

    private String text;

    public SystemMessageStruct() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text);
    }

    public static SystemMessageStruct from(SystemMessage message) {
        Objects.requireNonNull(message);
        SystemMessageStruct struct = new SystemMessageStruct();
        struct.setText(message.text());
        return struct;
    }

    public SystemMessage toSystemMessage() {
        return SystemMessage.from(this.getText());
    }

    @Override
    public ChatMessage toChatMessage() {
        return toSystemMessage();
    }
}

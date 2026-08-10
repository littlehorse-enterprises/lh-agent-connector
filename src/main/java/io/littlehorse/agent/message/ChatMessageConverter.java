package io.littlehorse.agent.message;

import dev.langchain4j.data.message.ChatMessage;

/** Common contract for LittleHorse Struct representations of LangChain4j chat messages. */
public interface ChatMessageConverter {
    ChatMessage toChatMessage();
}

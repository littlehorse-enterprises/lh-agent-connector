package io.littlehorse.agent.message;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import io.littlehorse.sdk.worker.LHStructDef;

import java.util.Arrays;
import java.util.Objects;

/**
 * Explicit LittleHorse Struct representation of LangChain4j {@link ChatResponse}.
 */
@LHStructDef(value = "chat-response", description = "A complete response from an LLM chat.")
public class ChatResponseStruct implements ChatMessageConverter {

    private AiMessageStruct aiMessage;
    private ChatResponseMetadataStruct metadata;

    public ChatResponseStruct() {}

    public AiMessageStruct getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(AiMessageStruct aiMessage) {
        this.aiMessage = Objects.requireNonNull(aiMessage);
    }

    public ChatResponseMetadataStruct getMetadata() {
        return metadata;
    }

    public void setMetadata(ChatResponseMetadataStruct metadata) {
        this.metadata = Objects.requireNonNull(metadata);
    }

    public static ChatResponseStruct from(ChatResponse chatResponse) {
        Objects.requireNonNull(chatResponse);
        ChatResponseStruct struct = new ChatResponseStruct();
        struct.setAiMessage(AiMessageStruct.from(chatResponse.aiMessage()));
        struct.setMetadata(ChatResponseMetadataStruct.from(chatResponse.metadata()));
        return struct;
    }

    @Override
    public ChatMessage toChatMessage() {
        return AiMessage.builder()
                .text(aiMessage.getText())
                .thinking(aiMessage.getThinking())
                .toolExecutionRequests(Arrays.stream(aiMessage.getToolExecutionRequests())
                        .map(ToolExecutionRequestStruct::toToolExecutionRequest)
                        .toList())
                .build();
    }
}

package io.littlehorse.agent.message;

import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Objects;

/** Explicit LittleHorse Struct representation of LangChain4j {@link ChatResponseMetadata}. */
@LHStructDef(value = "chat-response-metadata", description = "Metadata returned by an LLM chat.")
public class ChatResponseMetadataStruct {

    @LHStructField(isNullable = true)
    private String id;

    @LHStructField(isNullable = true)
    private String modelName;

    @LHStructField(isNullable = true)
    private TokenUsageStruct tokenUsage;

    @LHStructField(isNullable = true)
    private FinishReason finishReason;

    public ChatResponseMetadataStruct() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public TokenUsageStruct getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(TokenUsageStruct tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public static ChatResponseMetadataStruct from(ChatResponseMetadata metadata) {
        Objects.requireNonNull(metadata);
        ChatResponseMetadataStruct struct = new ChatResponseMetadataStruct();
        struct.setId(metadata.id());
        struct.setModelName(metadata.modelName());
        struct.setTokenUsage(
                metadata.tokenUsage() == null
                        ? null
                        : TokenUsageStruct.from(metadata.tokenUsage()));
        struct.setFinishReason(metadata.finishReason());
        return struct;
    }

    public ChatResponseMetadata toChatResponseMetadata() {
        return ChatResponseMetadata.builder()
                .id(this.getId())
                .modelName(this.getModelName())
                .tokenUsage(
                        this.getTokenUsage() == null
                                ? null
                                : TokenUsageStruct.toTokenUsage(this.getTokenUsage()))
                .finishReason(this.getFinishReason())
                .build();
    }
}

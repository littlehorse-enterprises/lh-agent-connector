package io.littlehorse.agent.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

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

    /** LittleHorse Struct representation of LangChain4j {@link TokenUsage}. */
    public static class TokenUsageStruct {

        @LHStructField(isNullable = true)
        private Integer inputTokenCount;

        @LHStructField(isNullable = true)
        private Integer outputTokenCount;

        @LHStructField(isNullable = true)
        private Integer totalTokenCount;

        public TokenUsageStruct() {}

        public Integer getInputTokenCount() {
            return inputTokenCount;
        }

        public void setInputTokenCount(Integer inputTokenCount) {
            this.inputTokenCount = inputTokenCount;
        }

        public Integer getOutputTokenCount() {
            return outputTokenCount;
        }

        public void setOutputTokenCount(Integer outputTokenCount) {
            this.outputTokenCount = outputTokenCount;
        }

        public Integer getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }

        public static TokenUsageStruct from(TokenUsage tokenUsage) {
            Objects.requireNonNull(tokenUsage);
            TokenUsageStruct struct = new TokenUsageStruct();
            struct.setInputTokenCount(tokenUsage.inputTokenCount());
            struct.setOutputTokenCount(tokenUsage.outputTokenCount());
            struct.setTotalTokenCount(tokenUsage.totalTokenCount());
            return struct;
        }

        public static TokenUsage toTokenUsage(TokenUsageStruct struct) {
            Objects.requireNonNull(struct);
            return new TokenUsage(
                    struct.getInputTokenCount(),
                    struct.getOutputTokenCount(),
                    struct.getTotalTokenCount());
        }
    }

    /** LittleHorse Struct representation of a LangChain4j {@link ToolExecutionRequest}. */
    public static class ToolExecutionRequestStruct {

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

    /** Explicit LittleHorse Struct representation of LangChain4j {@link ChatResponseMetadata}. */
    public static class ChatResponseMetadataStruct {

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

    /**
     * Explicit LittleHorse Struct representation of a LangChain4j {@link AiMessage}.
     */
    public static class AiMessageStruct {

        @LHStructField(isNullable = true)
        private String text;

        @LHStructField(isNullable = true)
        private String thinking;

        private ToolExecutionRequestStruct[] toolExecutionRequests =
                new ToolExecutionRequestStruct[0];

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
}

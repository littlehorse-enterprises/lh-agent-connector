package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.adapter.LHTypeAdapterRegistry;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

class ChatResponseStructTest {

    private static final LHTypeAdapterRegistry TYPE_ADAPTERS =
            LHTypeAdapterRegistry.from(List.of(new FinishReasonAdapter()));

    @Test
    void mapsEveryChatResponseFieldAndRestoresTheAiMessage() {
        ChatResponse original = completeChatResponse();

        ChatResponseStruct struct = ChatResponseStruct.from(original);

        assertThat(struct.toChatMessage()).isEqualTo(original.aiMessage());
        assertThat(struct.getMetadata().toChatResponseMetadata()).isEqualTo(original.metadata());
    }

    @Test
    void serializesEveryFieldAsALittleHorseStruct() {
        ChatResponse original = completeChatResponse();
        ChatResponseStruct struct = ChatResponseStruct.from(original);

        VariableValue serialized = LHLibUtil.objToVarVal(struct, TYPE_ADAPTERS);
        ChatResponseStruct restored = (ChatResponseStruct)
                LHLibUtil.varValToObj(serialized, ChatResponseStruct.class, TYPE_ADAPTERS);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName()).isEqualTo("chat-response");
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("aiMessage")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("metadata")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("aiMessage")
                        .getValue()
                        .getStruct()
                        .hasStructDefId())
                .isFalse();
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("metadata")
                        .getValue()
                        .getStruct()
                        .hasStructDefId())
                .isFalse();
        assertThat(restored.toChatMessage()).isEqualTo(original.aiMessage());
        assertThat(restored.getMetadata().toChatResponseMetadata()).isEqualTo(original.metadata());
    }

    @Test
    void definesBothChatResponseFieldsExplicitly() {
        var structDef =
                new LHStructDefType(ChatResponseStruct.class, TYPE_ADAPTERS).getInlineStructDef();

        assertThat(structDef.getFieldsMap()).containsOnlyKeys("aiMessage", "metadata");
        assertThat(structDef.getFieldsOrThrow("aiMessage").getFieldType().getDefinedTypeCase())
                .isEqualTo(TypeDefinition.DefinedTypeCase.INLINE_STRUCT_DEF);
        assertThat(structDef.getFieldsOrThrow("metadata").getFieldType().getDefinedTypeCase())
                .isEqualTo(TypeDefinition.DefinedTypeCase.INLINE_STRUCT_DEF);
    }

    private static ChatResponse completeChatResponse() {
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("tool-1")
                .name("test_tool")
                .arguments("{}")
                .build();
        AiMessage aiMessage = AiMessage.builder()
                .text("I will check the time.")
                .thinking("The time tool is required.")
                .toolExecutionRequests(List.of(toolRequest))
                .build();
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .id("response-1")
                .modelName("claude-sonnet-5")
                .tokenUsage(new TokenUsage(12, 7, 19))
                .finishReason(FinishReason.TOOL_EXECUTION)
                .build();
        return ChatResponse.builder().aiMessage(aiMessage).metadata(metadata).build();
    }

    @Nested
    class AiMessageStruct {

        @Test
        void mapsEveryAiMessageFieldToTheStruct() {
            AiMessage original = aiMessageWithEveryField();

            ChatResponseStruct.AiMessageStruct struct =
                    ChatResponseStruct.AiMessageStruct.from(original);

            assertThat(struct.getText()).isEqualTo("I will check the time.");
            assertThat(struct.getThinking()).isEqualTo("The time tool is required.");
            assertThat(struct.getToolExecutionRequests()).hasSize(1);
            assertThat(struct.getToolExecutionRequests()[0].getId()).isEqualTo("tool-1");
            assertThat(struct.getToolExecutionRequests()[0].getName()).isEqualTo("test_tool");
            assertThat(struct.getToolExecutionRequests()[0].getArguments()).isEqualTo("{}");
        }

        @Test
        void mapsNullableTextAndThinking() {
            AiMessage original = AiMessage.from(toolRequest());

            ChatResponseStruct.AiMessageStruct struct =
                    ChatResponseStruct.AiMessageStruct.from(original);

            assertThat(struct.getText()).isNull();
            assertThat(struct.getThinking()).isNull();
        }

        @Test
        void serializesEveryFieldAsAnInlineStruct() {
            VariableValue serialized = LHLibUtil.objToVarVal(
                            ChatResponseStruct.from(completeChatResponse()), TYPE_ADAPTERS)
                    .getStruct()
                    .getStruct()
                    .getFieldsOrThrow("aiMessage")
                    .getValue();

            assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
            assertThat(serialized.getStruct().hasStructDefId()).isFalse();
            assertThat(serialized
                            .getStruct()
                            .getStruct()
                            .getFieldsOrThrow("toolExecutionRequests")
                            .getValue()
                            .getArray()
                            .getItems(0)
                            .getStruct()
                            .hasStructDefId())
                    .isFalse();
        }

        @Test
        void definesAllAiMessageFieldsExplicitly() {
            var structDef = new LHStructDefType(ChatResponseStruct.class, TYPE_ADAPTERS)
                    .getInlineStructDef()
                    .getFieldsOrThrow("aiMessage")
                    .getFieldType()
                    .getInlineStructDef();

            assertThat(structDef.getFieldsMap())
                    .containsOnlyKeys("text", "thinking", "toolExecutionRequests");
            assertThat(structDef.getFieldsOrThrow("text").getIsNullable()).isTrue();
            assertThat(structDef.getFieldsOrThrow("thinking").getIsNullable()).isTrue();
            assertThat(structDef
                            .getFieldsOrThrow("toolExecutionRequests")
                            .getFieldType()
                            .getInlineArrayDef()
                            .getArrayType()
                            .getDefinedTypeCase())
                    .isEqualTo(TypeDefinition.DefinedTypeCase.INLINE_STRUCT_DEF);
        }

        private static AiMessage aiMessageWithEveryField() {
            return AiMessage.builder()
                    .text("I will check the time.")
                    .thinking("The time tool is required.")
                    .toolExecutionRequests(List.of(toolRequest()))
                    .build();
        }

        private static ToolExecutionRequest toolRequest() {
            return ToolExecutionRequest.builder()
                    .id("tool-1")
                    .name("test_tool")
                    .arguments("{}")
                    .build();
        }
    }

    @Nested
    class ChatResponseMetadataStruct {

        private static final LHTypeAdapterRegistry TYPE_ADAPTERS =
                LHTypeAdapterRegistry.from(List.of(new FinishReasonAdapter()));

        @Test
        void mapsEveryMetadataFieldToTheStructAndBack() {
            ChatResponseMetadata original = metadataWithEveryField();

            ChatResponseStruct.ChatResponseMetadataStruct struct =
                    ChatResponseStruct.ChatResponseMetadataStruct.from(original);
            ChatResponseMetadata restored = struct.toChatResponseMetadata();

            assertThat(struct.getId()).isEqualTo("response-1");
            assertThat(struct.getModelName()).isEqualTo("claude-sonnet-5");
            assertThat(struct.getTokenUsage().getInputTokenCount()).isEqualTo(12);
            assertThat(struct.getTokenUsage().getOutputTokenCount()).isEqualTo(7);
            assertThat(struct.getTokenUsage().getTotalTokenCount()).isEqualTo(19);
            assertThat(struct.getFinishReason()).isEqualTo(FinishReason.TOOL_EXECUTION);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        void mapsNullableMetadataFields() {
            ChatResponseMetadata original = ChatResponseMetadata.builder().build();

            ChatResponseStruct.ChatResponseMetadataStruct struct =
                    ChatResponseStruct.ChatResponseMetadataStruct.from(original);
            ChatResponseMetadata restored = struct.toChatResponseMetadata();

            assertThat(struct.getId()).isNull();
            assertThat(struct.getModelName()).isNull();
            assertThat(struct.getTokenUsage()).isNull();
            assertThat(struct.getFinishReason()).isNull();
            assertThat(restored).isEqualTo(original);
        }

        @Test
        void serializesEveryFieldAsAnInlineStruct() {
            VariableValue serialized = LHLibUtil.objToVarVal(
                            ChatResponseStruct.from(completeChatResponse()), TYPE_ADAPTERS)
                    .getStruct()
                    .getStruct()
                    .getFieldsOrThrow("metadata")
                    .getValue();

            assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
            assertThat(serialized.getStruct().hasStructDefId()).isFalse();
            assertThat(serialized
                            .getStruct()
                            .getStruct()
                            .getFieldsOrThrow("tokenUsage")
                            .getValue()
                            .getStruct()
                            .hasStructDefId())
                    .isFalse();
        }

        @Test
        void definesAllMetadataFieldsExplicitly() {
            var structDef = new LHStructDefType(ChatResponseStruct.class, TYPE_ADAPTERS)
                    .getInlineStructDef()
                    .getFieldsOrThrow("metadata")
                    .getFieldType()
                    .getInlineStructDef();

            assertThat(structDef.getFieldsMap())
                    .containsOnlyKeys("id", "modelName", "tokenUsage", "finishReason");
            assertThat(structDef.getFieldsOrThrow("id").getIsNullable()).isTrue();
            assertThat(structDef.getFieldsOrThrow("modelName").getIsNullable()).isTrue();
            assertThat(structDef.getFieldsOrThrow("tokenUsage").getIsNullable()).isTrue();
            assertThat(structDef.getFieldsOrThrow("finishReason").getIsNullable())
                    .isTrue();
            assertThat(structDef.getFieldsOrThrow("finishReason").getFieldType().getPrimitiveType())
                    .isEqualTo(VariableType.STR);
            assertThat(structDef.getFieldsOrThrow("tokenUsage").getFieldType().getDefinedTypeCase())
                    .isEqualTo(TypeDefinition.DefinedTypeCase.INLINE_STRUCT_DEF);
        }

        private static ChatResponseMetadata metadataWithEveryField() {
            return ChatResponseMetadata.builder()
                    .id("response-1")
                    .modelName("claude-sonnet-5")
                    .tokenUsage(new TokenUsage(12, 7, 19))
                    .finishReason(FinishReason.TOOL_EXECUTION)
                    .build();
        }
    }
}

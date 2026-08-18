package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.adapter.LHTypeAdapterRegistry;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Test;

import java.util.List;

class ChatResponseMetadataStructTest {

    private static final LHTypeAdapterRegistry TYPE_ADAPTERS =
            LHTypeAdapterRegistry.from(List.of(new FinishReasonAdapter()));

    @Test
    void mapsEveryMetadataFieldToTheStructAndBack() {
        ChatResponseMetadata original = metadataWithEveryField();

        ChatResponseMetadataStruct struct = ChatResponseMetadataStruct.from(original);
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

        ChatResponseMetadataStruct struct = ChatResponseMetadataStruct.from(original);
        ChatResponseMetadata restored = struct.toChatResponseMetadata();

        assertThat(struct.getId()).isNull();
        assertThat(struct.getModelName()).isNull();
        assertThat(struct.getTokenUsage()).isNull();
        assertThat(struct.getFinishReason()).isNull();
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void serializesEveryFieldAsALittleHorseStruct() {
        ChatResponseMetadata original = metadataWithEveryField();
        ChatResponseMetadataStruct struct = ChatResponseMetadataStruct.from(original);

        VariableValue serialized = LHLibUtil.objToVarVal(struct, TYPE_ADAPTERS);
        ChatResponseMetadataStruct restored = (ChatResponseMetadataStruct)
                LHLibUtil.varValToObj(serialized, ChatResponseMetadataStruct.class, TYPE_ADAPTERS);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName())
                .isEqualTo("chat-response-metadata");
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("tokenUsage")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(restored.toChatResponseMetadata()).isEqualTo(original);
    }

    @Test
    void definesAllMetadataFieldsExplicitly() {
        var structDef = new LHStructDefType(ChatResponseMetadataStruct.class, TYPE_ADAPTERS)
                .getInlineStructDef();

        assertThat(structDef.getFieldsMap())
                .containsOnlyKeys("id", "modelName", "tokenUsage", "finishReason");
        assertThat(structDef.getFieldsOrThrow("id").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("modelName").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("tokenUsage").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("finishReason").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("finishReason").getFieldType().getPrimitiveType())
                .isEqualTo(VariableType.STR);
        assertThat(structDef
                        .getFieldsOrThrow("tokenUsage")
                        .getFieldType()
                        .getStructDefId()
                        .getName())
                .isEqualTo("token-usage");
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

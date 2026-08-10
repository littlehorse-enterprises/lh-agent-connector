package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Test;

import java.util.List;

class AiMessageStructTest {

    @Test
    void mapsEveryAiMessageFieldToTheStruct() {
        AiMessage original = aiMessageWithEveryField();

        AiMessageStruct struct = AiMessageStruct.from(original);

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

        AiMessageStruct struct = AiMessageStruct.from(original);

        assertThat(struct.getText()).isNull();
        assertThat(struct.getThinking()).isNull();
    }

    @Test
    void serializesEveryFieldAsALittleHorseStruct() {
        AiMessage original = aiMessageWithEveryField();
        AiMessageStruct struct = AiMessageStruct.from(original);

        VariableValue serialized = LHLibUtil.objToVarVal(struct);
        AiMessageStruct restored =
                (AiMessageStruct) LHLibUtil.varValToObj(serialized, AiMessageStruct.class);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName()).isEqualTo("ai-message");
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("toolExecutionRequests")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.ARRAY);
        assertThat(restored.getText()).isEqualTo(original.text());
        assertThat(restored.getThinking()).isEqualTo(original.thinking());
        assertThat(restored.getToolExecutionRequests()).hasSize(1);
        assertThat(restored.getToolExecutionRequests()[0].getId()).isEqualTo("tool-1");
        assertThat(restored.getToolExecutionRequests()[0].getName()).isEqualTo("test_tool");
        assertThat(restored.getToolExecutionRequests()[0].getArguments()).isEqualTo("{}");
    }

    @Test
    void definesAllAiMessageFieldsExplicitly() {
        var structDef = new LHStructDefType(AiMessageStruct.class).getInlineStructDef();

        assertThat(structDef.getFieldsMap())
                .containsOnlyKeys("text", "thinking", "toolExecutionRequests");
        assertThat(structDef.getFieldsOrThrow("text").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("thinking").getIsNullable()).isTrue();
        assertThat(structDef
                        .getFieldsOrThrow("toolExecutionRequests")
                        .getFieldType()
                        .getInlineArrayDef()
                        .getArrayType()
                        .getStructDefId()
                        .getName())
                .isEqualTo("tool-execution-request");
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

package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Test;

class ToolExecutionResultMessageStructTest {

    @Test
    void mapsEveryToolResultFieldToTheStructAndBack() {
        ToolExecutionResultMessage original = messageWithEveryField();

        ToolExecutionResultMessageStruct struct = ToolExecutionResultMessageStruct.from(original);
        ToolExecutionResultMessage restored = (ToolExecutionResultMessage) struct.toChatMessage();

        assertThat(struct.getId()).isEqualTo("tool-1");
        assertThat(struct.getToolName()).isEqualTo("test_tool");
        assertThat(struct.getContents()).hasSize(2);
        assertThat(struct.getContents()[0].getText()).isEqualTo("first result");
        assertThat(struct.getContents()[1].getText()).isEqualTo("second result");
        assertThat(struct.getIsError()).isTrue();
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void mapsNullableFields() {
        ToolExecutionResultMessage original = ToolExecutionResultMessage.builder()
                .contents(TextContent.from("result"))
                .build();

        ToolExecutionResultMessageStruct struct = ToolExecutionResultMessageStruct.from(original);
        ToolExecutionResultMessage restored = (ToolExecutionResultMessage) struct.toChatMessage();

        assertThat(struct.getId()).isNull();
        assertThat(struct.getToolName()).isNull();
        assertThat(struct.getIsError()).isNull();
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void rejectsNonTextContentWithoutPolymorphicMapping() {
        Content nonTextContent = () -> ContentType.IMAGE;
        ToolExecutionResultMessage message =
                ToolExecutionResultMessage.builder().contents(nonTextContent).build();

        assertThatThrownBy(() -> ToolExecutionResultMessageStruct.from(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only TextContent is supported in tool execution result contents");
    }

    @Test
    void serializesEveryFieldAsALittleHorseStruct() {
        ToolExecutionResultMessage original = messageWithEveryField();
        ToolExecutionResultMessageStruct struct = ToolExecutionResultMessageStruct.from(original);

        VariableValue serialized = LHLibUtil.objToVarVal(struct);
        ToolExecutionResultMessageStruct restored = (ToolExecutionResultMessageStruct)
                LHLibUtil.varValToObj(serialized, ToolExecutionResultMessageStruct.class);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName())
                .isEqualTo("tool-execution-result-message");
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("contents")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.ARRAY);
        assertThat(restored.toChatMessage()).isEqualTo(original);
    }

    @Test
    void definesAllToolResultFieldsExplicitly() {
        var structDef =
                new LHStructDefType(ToolExecutionResultMessageStruct.class).getInlineStructDef();

        assertThat(structDef.getFieldsMap())
                .containsOnlyKeys("id", "toolName", "contents", "isError");
        assertThat(structDef.getFieldsOrThrow("id").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("toolName").getIsNullable()).isTrue();
        assertThat(structDef.getFieldsOrThrow("isError").getIsNullable()).isTrue();
        assertThat(structDef
                        .getFieldsOrThrow("contents")
                        .getFieldType()
                        .getInlineArrayDef()
                        .getArrayType()
                        .getStructDefId()
                        .getName())
                .isEqualTo("text-content");
    }

    private static ToolExecutionResultMessage messageWithEveryField() {
        return ToolExecutionResultMessage.builder()
                .id("tool-1")
                .toolName("test_tool")
                .contents(TextContent.from("first result"), TextContent.from("second result"))
                .isError(true)
                .build();
    }
}

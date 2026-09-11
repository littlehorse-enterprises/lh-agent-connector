package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class UserMessageStructTest {

    @Test
    void mapsEverySupportedUserMessageFieldToTheStructAndBack() {
        UserMessage original = UserMessage.builder()
                .name("Mateo")
                .contents(List.of(TextContent.from("first"), TextContent.from("second")))
                .attributes(Map.of("ignored", true))
                .build();

        UserMessageStruct struct = UserMessageStruct.from(original);
        UserMessage restored = (UserMessage) struct.toChatMessage();

        assertThat(struct.getName()).isEqualTo("Mateo");
        assertThat(struct.getContents()).hasSize(2);
        assertThat(struct.getContents()[0].getText()).isEqualTo("first");
        assertThat(struct.getContents()[1].getText()).isEqualTo("second");
        assertThat(restored.name()).isEqualTo(original.name());
        assertThat(restored.contents()).isEqualTo(original.contents());
        assertThat(restored.attributes()).isEmpty();
        assertThat(struct.toChatMessage()).isEqualTo(restored);
    }

    @Test
    void mapsNullableName() {
        UserMessage original = UserMessage.from("hello");

        UserMessageStruct struct = UserMessageStruct.from(original);
        UserMessage restored = (UserMessage) struct.toChatMessage();

        assertThat(struct.getName()).isNull();
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void rejectsNonTextContentWithoutPolymorphicMapping() {
        Content nonTextContent = () -> ContentType.IMAGE;
        UserMessage message = UserMessage.from(nonTextContent);

        assertThatThrownBy(() -> UserMessageStruct.from(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only TextContent is supported in user message contents");
    }

    @Test
    void serializesEveryFieldAsALittleHorseStruct() {
        UserMessageStruct struct = UserMessageStruct.from(UserMessage.from("Mateo", "hello"));

        VariableValue serialized = LHLibUtil.objToVarVal(struct);
        UserMessageStruct restored =
                (UserMessageStruct) LHLibUtil.varValToObj(serialized, UserMessageStruct.class);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName()).isEqualTo("user-message");
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("contents")
                        .getValue()
                        .getValueCase())
                .isEqualTo(VariableValue.ValueCase.ARRAY);
        assertThat(serialized
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("contents")
                        .getValue()
                        .getArray()
                        .getItems(0)
                        .getStruct()
                        .hasStructDefId())
                .isFalse();
        assertThat(restored.getName()).isEqualTo(struct.getName());
        assertThat(restored.toChatMessage()).isEqualTo(struct.toChatMessage());
    }

    @Test
    void definesAllRequestedFieldsExplicitly() {
        var structDef = new LHStructDefType(UserMessageStruct.class).getInlineStructDef();

        assertThat(structDef.getFieldsMap()).containsOnlyKeys("name", "contents");
        assertThat(structDef.getFieldsOrThrow("name").getIsNullable()).isTrue();
        assertThat(structDef
                        .getFieldsOrThrow("contents")
                        .getFieldType()
                        .getInlineArrayDef()
                        .getArrayType()
                        .getDefinedTypeCase())
                .isEqualTo(TypeDefinition.DefinedTypeCase.INLINE_STRUCT_DEF);
    }
}

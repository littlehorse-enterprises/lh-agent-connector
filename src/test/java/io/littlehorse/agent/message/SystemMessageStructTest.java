package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.SystemMessage;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

import org.junit.jupiter.api.Test;

class SystemMessageStructTest {

    @Test
    void mapsTextToTheStructAndBack() {
        SystemMessage original = SystemMessage.from("You are a helpful assistant.");

        SystemMessageStruct struct = SystemMessageStruct.from(original);
        SystemMessage restored = struct.toSystemMessage();

        assertThat(struct.getText()).isEqualTo(original.text());
        assertThat(restored).isEqualTo(original);
        assertThat(struct.toChatMessage()).isEqualTo(original);
    }

    @Test
    void serializesAsALittleHorseStruct() {
        SystemMessageStruct struct =
                SystemMessageStruct.from(SystemMessage.from("You are concise."));

        VariableValue serialized = LHLibUtil.objToVarVal(struct);
        SystemMessageStruct restored =
                (SystemMessageStruct) LHLibUtil.varValToObj(serialized, SystemMessageStruct.class);

        assertThat(serialized.getValueCase()).isEqualTo(VariableValue.ValueCase.STRUCT);
        assertThat(serialized.getStruct().getStructDefId().getName()).isEqualTo("system-message");
        assertThat(restored.toSystemMessage()).isEqualTo(struct.toSystemMessage());
    }

    @Test
    void definesTextExplicitly() {
        var structDef = new LHStructDefType(SystemMessageStruct.class).getInlineStructDef();

        assertThat(structDef.getFieldsMap()).containsOnlyKeys("text");
        assertThat(structDef.getFieldsOrThrow("text").getFieldType().getPrimitiveType())
                .isEqualTo(VariableType.STR);
    }
}

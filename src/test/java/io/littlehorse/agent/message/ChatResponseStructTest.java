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
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.internal.structdefutil.LHStructDefType;

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
        assertThat(restored.toChatMessage()).isEqualTo(original.aiMessage());
        assertThat(restored.getMetadata().toChatResponseMetadata()).isEqualTo(original.metadata());
    }

    @Test
    void definesBothChatResponseFieldsExplicitly() {
        var structDef =
                new LHStructDefType(ChatResponseStruct.class, TYPE_ADAPTERS).getInlineStructDef();

        assertThat(structDef.getFieldsMap()).containsOnlyKeys("aiMessage", "metadata");
        assertThat(structDef
                        .getFieldsOrThrow("aiMessage")
                        .getFieldType()
                        .getStructDefId()
                        .getName())
                .isEqualTo("ai-message");
        assertThat(structDef
                        .getFieldsOrThrow("metadata")
                        .getFieldType()
                        .getStructDefId()
                        .getName())
                .isEqualTo("chat-response-metadata");
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
}

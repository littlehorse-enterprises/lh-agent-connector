package io.littlehorse.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;

import io.littlehorse.agent.message.ChatResponseStruct;
import io.littlehorse.agent.message.SystemMessageStruct;
import io.littlehorse.agent.message.ToolExecutionResultMessageStruct;
import io.littlehorse.agent.message.UserMessageStruct;
import io.littlehorse.sdk.common.proto.ScheduledTask;
import io.littlehorse.sdk.worker.CheckpointContext;
import io.littlehorse.sdk.worker.CheckpointableFunction;
import io.littlehorse.sdk.worker.WorkerContext;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

class CheckpointMessagesTest {

    @Test
    void checkpointsAndAccumulatesEverySupportedMessageType() {
        RecordingWorkerContext context = new RecordingWorkerContext();
        CheckpointMessages messages = new CheckpointMessages(context);
        SystemMessage systemMessage = SystemMessage.from("Follow the instructions.");
        UserMessage userMessage = UserMessage.from("hello");
        AiMessage aiMessage = AiMessage.from(toolRequest());
        ChatResponse chatResponse = chatResponse(aiMessage);
        ToolExecutionResultMessage toolResult =
                ToolExecutionResultMessage.from(toolRequest(), "result");

        assertThat(messages.addSystemMessage(() -> systemMessage)).isEqualTo(systemMessage);
        assertThat(messages.addUserMessage(() -> userMessage)).isEqualTo(userMessage);
        assertThat(messages.addChatResponse(() -> chatResponse)).isEqualTo(aiMessage);
        assertThat(messages.addToolExecutionResultMessage(() -> toolResult)).isEqualTo(toolResult);

        assertThat(messages.snapshot())
                .containsExactly(systemMessage, userMessage, aiMessage, toolResult);
        assertThat(context.checkpoints.get(0)).isInstanceOf(SystemMessageStruct.class);
        assertThat(context.checkpoints.get(1)).isInstanceOf(UserMessageStruct.class);
        ChatResponseStruct checkpoint =
                assertInstanceOf(ChatResponseStruct.class, context.checkpoints.get(2));
        assertThat(checkpoint.getMetadata().getId()).isEqualTo("response-1");
        assertThat(context.checkpoints.get(3)).isInstanceOf(ToolExecutionResultMessageStruct.class);
    }

    @Test
    void restoresEveryMessageWithoutExecutingSuppliers() {
        SystemMessage systemMessage = SystemMessage.from("restored system");
        UserMessage userMessage = UserMessage.from("restored user");
        AiMessage aiMessage = AiMessage.from("restored AI");
        ToolExecutionResultMessage toolResult =
                ToolExecutionResultMessage.from(toolRequest(), "restored tool result");
        RecordingWorkerContext context = new RecordingWorkerContext(
                SystemMessageStruct.from(systemMessage),
                UserMessageStruct.from(userMessage),
                ChatResponseStruct.from(chatResponse(aiMessage)),
                ToolExecutionResultMessageStruct.from(toolResult));
        CheckpointMessages messages = new CheckpointMessages(context);

        assertThat(messages.addSystemMessage(CheckpointMessagesTest::fail))
                .isEqualTo(systemMessage);
        assertThat(messages.addUserMessage(CheckpointMessagesTest::fail)).isEqualTo(userMessage);
        assertThat(messages.addChatResponse(CheckpointMessagesTest::fail)).isEqualTo(aiMessage);
        assertThat(messages.addToolExecutionResultMessage(CheckpointMessagesTest::fail))
                .isEqualTo(toolResult);

        assertThat(messages.snapshot())
                .containsExactly(systemMessage, userMessage, aiMessage, toolResult);
        assertThat(context.checkpoints).hasSize(4);
        assertThat(context.restoredValues).isEmpty();
    }

    @Test
    void snapshotsAreImmutableAndIndependentOfLaterMessages() {
        CheckpointMessages messages = new CheckpointMessages(new RecordingWorkerContext());
        UserMessage userMessage = messages.addUserMessage(() -> UserMessage.from("hello"));
        List<ChatMessage> firstSnapshot = messages.snapshot();

        assertThatThrownBy(() -> firstSnapshot.add(userMessage))
                .isInstanceOf(UnsupportedOperationException.class);

        messages.addChatResponse(() -> chatResponse(AiMessage.from("response")));

        assertThat(firstSnapshot).containsExactly(userMessage);
        assertThat(messages.snapshot()).hasSize(2);
    }

    private static ToolExecutionRequest toolRequest() {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name("test-tool")
                .arguments("{}")
                .build();
    }

    private static ChatResponse chatResponse(AiMessage aiMessage) {
        return ChatResponse.builder()
                .aiMessage(aiMessage)
                .metadata(ChatResponseMetadata.builder().id("response-1").build())
                .build();
    }

    private static <T> T fail() {
        throw new AssertionError("A restored checkpoint must not execute its supplier");
    }

    private static final class RecordingWorkerContext extends WorkerContext {

        private final Deque<Object> restoredValues;
        private final List<Object> checkpoints = new ArrayList<>();

        private RecordingWorkerContext(Object... restoredValues) {
            super(ScheduledTask.getDefaultInstance(), null, null);
            this.restoredValues = new ArrayDeque<>(List.of(restoredValues));
        }

        @Override
        public <T> T executeAndCheckpoint(
                CheckpointableFunction<T> checkpointableFunction, Class<T> clazz) {
            T result;
            if (restoredValues.isEmpty()) {
                result = checkpointableFunction.run(new CheckpointContext());
            } else {
                Object restored = restoredValues.removeFirst();
                assertThat(restored).isExactlyInstanceOf(clazz);
                result = clazz.cast(restored);
            }
            assertThat(result).isInstanceOf(clazz);
            checkpoints.add(result);
            return result;
        }
    }
}

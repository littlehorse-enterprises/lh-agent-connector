package io.littlehorse.agent.execution;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import io.littlehorse.agent.message.ChatMessageConverter;
import io.littlehorse.agent.message.ChatResponseStruct;
import io.littlehorse.agent.message.SystemMessageStruct;
import io.littlehorse.agent.message.ToolExecutionResultMessageStruct;
import io.littlehorse.agent.message.UserMessageStruct;
import io.littlehorse.sdk.worker.WorkerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CheckpointMessages {

    private static final Logger LOG = LoggerFactory.getLogger(CheckpointMessages.class);

    private final WorkerContext context;
    private final List<ChatMessage> messages = new ArrayList<>();

    public CheckpointMessages(WorkerContext context) {
        this.context = context;
    }

    public List<ChatMessage> snapshot() {
        return List.copyOf(messages);
    }

    public UserMessage addUserMessage(Supplier<UserMessage> supplier) {
        return (UserMessage)
                add(() -> UserMessageStruct.from(supplier.get()), UserMessageStruct.class);
    }

    public SystemMessage addSystemMessage(Supplier<SystemMessage> supplier) {
        return (SystemMessage)
                add(() -> SystemMessageStruct.from(supplier.get()), SystemMessageStruct.class);
    }

    public AiMessage addChatResponse(Supplier<ChatResponse> supplier) {
        return (AiMessage) add(
                () -> {
                    ChatResponse response = supplier.get();
                    LOG.debug("AI message response before checkpoint: {}", response);
                    return ChatResponseStruct.from(response);
                },
                ChatResponseStruct.class);
    }

    public ToolExecutionResultMessage addToolExecutionResultMessage(
            Supplier<ToolExecutionResultMessage> supplier) {
        return (ToolExecutionResultMessage) add(
                () -> {
                    ToolExecutionResultMessage response = supplier.get();
                    LOG.debug("Tool response before checkpoint: {}", response);
                    return ToolExecutionResultMessageStruct.from(response);
                },
                ToolExecutionResultMessageStruct.class);
    }

    private <T extends ChatMessageConverter> ChatMessage add(Supplier<T> supplier, Class<T> clazz) {
        ChatMessage chatMessage =
                context.executeAndCheckpoint(_ -> supplier.get(), clazz).toChatMessage();
        messages.add(chatMessage);
        return chatMessage;
    }
}

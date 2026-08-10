package io.littlehorse.agent.execution;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;

import io.littlehorse.agent.mcp.ToolsManager;
import io.littlehorse.agent.message.UserMessageTemplateRenderer;
import io.littlehorse.agent.structuredoutput.StructDefResolver;
import io.littlehorse.agent.structuredoutput.StructDefSnapshot;
import io.littlehorse.agent.structuredoutput.StructTransformer;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.worker.WorkerContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

/**
 * A single-turn agent backed by LangChain4j's low-level {@link ChatModel} API.
 *
 * <p>Each LittleHorse task execution is independent. It starts with one user message and maintains
 * only the temporary message history required to complete low-level tool calls.
 */
@ApplicationScoped
public class AgentExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutor.class);

    private static final int MAX_CHAT_ROUNDS = 10;

    private final ChatModel chatModel;
    private final Optional<SystemMessage> systemMessage;
    private final ToolsManager toolsManager;
    private final Instance<StructDefResolver> structDefResolver;
    private final Instance<UserMessageTemplateRenderer> userMessageTemplateRenderer;

    public AgentExecutor(
            ChatModel chatModel,
            Optional<SystemMessage> systemMessage,
            ToolsManager toolsManager,
            Instance<StructDefResolver> structDefResolver,
            Instance<UserMessageTemplateRenderer> userMessageTemplateRenderer) {
        this.chatModel = Objects.requireNonNull(chatModel);
        this.systemMessage = Objects.requireNonNull(systemMessage);
        this.toolsManager = Objects.requireNonNull(toolsManager);
        this.structDefResolver = Objects.requireNonNull(structDefResolver);
        this.userMessageTemplateRenderer = Objects.requireNonNull(userMessageTemplateRenderer);
    }

    public String textToText(String userMessage, WorkerContext context) {
        return executeAgent(() -> UserMessage.from(userMessage), ResponseFormat.TEXT, context);
    }

    public InlineStruct textToStruct(String userMessage, WorkerContext context) {
        StructDefSnapshot output = structDefResolver.get().resolve();
        ResponseFormat format = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(output.jsonSchema())
                .build();
        String response = executeAgent(() -> UserMessage.from(userMessage), format, context);
        return StructTransformer.fromJson(response, output.root(), output.definitions());
    }

    public InlineStruct structToStruct(InlineStruct input, WorkerContext context) {
        StructDefSnapshot output = structDefResolver.get().resolve();
        ResponseFormat format = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(output.jsonSchema())
                .build();
        String response = executeAgent(() -> renderedUserMessage(input), format, context);
        return StructTransformer.fromJson(response, output.root(), output.definitions());
    }

    public String structToText(InlineStruct input, WorkerContext context) {
        return executeAgent(() -> renderedUserMessage(input), ResponseFormat.TEXT, context);
    }

    private String executeAgent(
            Supplier<UserMessage> userMessage, ResponseFormat format, WorkerContext context) {
        CheckpointMessages messages = new CheckpointMessages(context);
        systemMessage.ifPresent(systemMessage -> messages.addSystemMessage(() -> systemMessage));
        messages.addUserMessage(userMessage);

        for (int round = 0; round < MAX_CHAT_ROUNDS; round++) {
            AiMessage aiMessage = messages.addChatResponse(() -> chat(messages.snapshot(), format));

            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text();
            }

            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                messages.addToolExecutionResultMessage(() -> executeTool(toolRequest));
            }
        }

        throw new IllegalStateException(
                "The model exceeded the limit of " + MAX_CHAT_ROUNDS + " chat rounds");
    }

    private UserMessage renderedUserMessage(InlineStruct input) {
        return UserMessage.from(userMessageTemplateRenderer.get().render(input));
    }

    private ChatResponse chat(List<ChatMessage> messages, ResponseFormat format) {
        // ToolsManager is called within the checkpoint so that retries do not call the tools again
        List<ToolSpecification> specifications = toolsManager.getToolsSpecification();
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .responseFormat(format)
                .toolSpecifications(specifications)
                .build();
        return chatModel.chat(request);
    }

    private ToolExecutionResultMessage executeTool(ToolExecutionRequest request) {
        // ToolsManager is called within the checkpoint so that retries do not call the tools again
        ToolExecutor executor = toolsManager
                .getToolExecutor(request.name())
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown MCP tool: " + request.name()));

        ToolExecutionResult result =
                executor.executeWithContext(request, InvocationContext.builder().build());

        result.resultContents().stream()
                .filter(content -> !(content instanceof TextContent))
                .findFirst()
                .ifPresent(content -> {
                    throw new IllegalArgumentException("MCP tool '" + request.name()
                            + "' returned unsupported non-text content: " + content);
                });

        return ToolExecutionResultMessage.builder()
                .id(request.id())
                .toolName(request.name())
                .contents(result.resultContents())
                .isError(result.isError())
                .build();
    }
}

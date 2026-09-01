package io.littlehorse.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.service.tool.ToolExecutionResult;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.mcp.ToolsManager;
import io.littlehorse.agent.message.ChatResponseStruct;
import io.littlehorse.agent.message.SystemMessageStruct;
import io.littlehorse.agent.message.ToolExecutionResultMessageStruct;
import io.littlehorse.agent.message.UserMessageStruct;
import io.littlehorse.agent.message.UserMessageTemplateRenderer;
import io.littlehorse.agent.structuredoutput.StructDefResolver;
import io.littlehorse.agent.structuredoutput.StructDefSnapshot;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.ScheduledTask;
import io.littlehorse.sdk.common.proto.Struct;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.worker.CheckpointContext;
import io.littlehorse.sdk.worker.CheckpointableFunction;
import io.littlehorse.sdk.worker.WorkerContext;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

class AgentExecutorTest {

    @Test
    void checkpointsAChatWithoutToolCalls() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("The model response"))
                        .metadata(ChatResponseMetadata.builder()
                                .id("response-1")
                                .modelName("test-model")
                                .build())
                        .build());

        TestWorkerContext context = new TestWorkerContext();
        String response = agentExecutor(model, Optional.empty(), emptyToolsManager())
                .textToText("Hello", context);

        assertThat(response).isEqualTo("The model response");
        assertThat(context.checkpointCount).isEqualTo(2);
        UserMessageStruct userInput =
                assertInstanceOf(UserMessageStruct.class, context.checkpoints.getFirst());
        assertThat(userInput.getContents()[0].getText()).isEqualTo("Hello");
        ChatResponseStruct chatOutput =
                assertInstanceOf(ChatResponseStruct.class, context.checkpoints.get(1));
        assertThat(chatOutput.getAiMessage().getText()).isEqualTo("The model response");
        assertThat(chatOutput.getMetadata().getId()).isEqualTo("response-1");
        assertThat(chatOutput.getMetadata().getModelName()).isEqualTo("test-model");
        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(requestCaptor.capture());
        ChatRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.messages()).hasSize(1);
        assertThat(capturedRequest.toolSpecifications()).isEmpty();
        UserMessage userMessage =
                assertInstanceOf(UserMessage.class, capturedRequest.messages().getFirst());
        assertThat(userMessage.singleText()).isEqualTo("Hello");
    }

    @Test
    void serializesAnInlineStructAsTheUserMessage() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("The struct response"))
                        .build());
        InlineStruct address = InlineStruct.newBuilder()
                .putFields("city", field(VariableValue.newBuilder().setStr("Quito")))
                .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("name", field(VariableValue.newBuilder().setStr("Ada")))
                .putFields("age", field(VariableValue.newBuilder().setInt(36)))
                .putFields("active", field(VariableValue.newBuilder().setBool(true)))
                .putFields(
                        "preferences",
                        field(VariableValue.newBuilder().setJsonObj("{\"tone\":\"concise\"}")))
                .putFields(
                        "tags",
                        field(VariableValue.newBuilder()
                                .setArray(io.littlehorse.sdk.common.proto.Array.newBuilder()
                                        .addItems(VariableValue.newBuilder().setStr("agent"))
                                        .addItems(VariableValue.getDefaultInstance()))))
                .putFields(
                        "address",
                        field(VariableValue.newBuilder()
                                .setStruct(Struct.newBuilder()
                                        .setStructDefId(StructDefId.newBuilder().setName("address"))
                                        .setStruct(address))))
                .build();
        TestWorkerContext context = new TestWorkerContext();

        String response = agentExecutor(model, Optional.empty(), emptyToolsManager())
                .structToText(input, context);

        assertThat(response).isEqualTo("The struct response");
        UserMessageStruct checkpoint =
                assertInstanceOf(UserMessageStruct.class, context.checkpoints.getFirst());
        String checkpointJson = checkpoint.getContents()[0].getText();
        assertThat(checkpointJson)
                .startsWith("{")
                .endsWith("}")
                .contains(
                        "\"name\":\"Ada\"",
                        "\"age\":36",
                        "\"active\":true",
                        "\"preferences\":{\"tone\":\"concise\"}",
                        "\"tags\":[\"agent\",null]",
                        "\"address\":{\"city\":\"Quito\"}");

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(requestCaptor.capture());
        UserMessage userMessage = assertInstanceOf(
                UserMessage.class, requestCaptor.getValue().messages().getFirst());
        assertThat(userMessage.singleText()).isEqualTo(checkpointJson);
    }

    @Test
    void rendersTheConfiguredUserMessageTemplateForStructInput() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("The struct response"))
                        .build());
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("name", field(VariableValue.newBuilder().setStr("Ada")))
                .putFields(
                        "address",
                        field(VariableValue.newBuilder()
                                .setJsonObj("{\"city\":\"Quito\",\"zip\":170101}")))
                .build();
        AgentExecutor executor = new AgentExecutor(
                model,
                mockAgentConfiguration(10),
                Optional.empty(),
                emptyToolsManager(),
                unavailableStructDefResolver(),
                userMessageTemplateRenderer(
                        "Customer {{struct.name}} lives at {{struct.address}}."));
        TestWorkerContext context = new TestWorkerContext();

        executor.structToText(input, context);

        UserMessageStruct checkpoint =
                assertInstanceOf(UserMessageStruct.class, context.checkpoints.getFirst());
        assertThat(checkpoint.getContents()[0].getText())
                .isEqualTo("Customer Ada lives at {\"city\":\"Quito\",\"zip\":170101}.");
    }

    @Test
    void doesNotRenderStructInputWhenTheUserMessageCheckpointIsRestored() {
        UserMessageStruct restored = UserMessageStruct.from(UserMessage.from("Already rendered"));
        TestWorkerContext context = new TestWorkerContext(
                restored, chatResponseStruct(AiMessage.from("Restored response")));
        ChatModel model = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        Instance<UserMessageTemplateRenderer> renderer = mock(Instance.class);
        AgentExecutor executor = new AgentExecutor(
                model,
                mockAgentConfiguration(10),
                Optional.empty(),
                emptyToolsManager(),
                unavailableStructDefResolver(),
                renderer);

        String response = executor.structToText(InlineStruct.getDefaultInstance(), context);

        assertThat(response).isEqualTo("Restored response");
        verifyNoInteractions(renderer, model);
    }

    @Test
    void prependsAndCheckpointsTheConfiguredSystemMessage() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("The model response"))
                        .build());
        SystemMessage systemMessage = SystemMessage.from("You are a helpful assistant.");
        TestWorkerContext context = new TestWorkerContext();

        String response = agentExecutor(model, Optional.of(systemMessage), emptyToolsManager())
                .textToText("Hello", context);

        assertThat(response).isEqualTo("The model response");
        assertThat(context.checkpointCount).isEqualTo(3);
        SystemMessageStruct systemInput =
                assertInstanceOf(SystemMessageStruct.class, context.checkpoints.get(0));
        assertThat(systemInput.getText()).isEqualTo(systemMessage.text());
        assertThat(context.checkpoints.get(1)).isInstanceOf(UserMessageStruct.class);
        assertThat(context.checkpoints.get(2)).isInstanceOf(ChatResponseStruct.class);
        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(requestCaptor.capture());
        assertThat(requestCaptor.getValue().messages())
                .containsExactly(systemMessage, UserMessage.from("Hello"));
    }

    @Test
    void sendsConfiguredMcpToolsToTheModelAndCheckpointsTheirExecution() {
        String toolName = "remote_search";
        ToolSpecification specification = ToolSpecification.builder()
                .name(toolName)
                .description("Searches a remote MCP server")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        McpClient mcpClient =
                mcpClient("remote", List.of(specification), ignored -> ToolExecutionResult.builder()
                        .resultText("MCP search result")
                        .isError(false)
                        .build());
        ToolsManager toolsManager =
                new ToolsManager(McpToolProvider.builder().mcpClients(mcpClient).build());
        AtomicInteger chatCount = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class))).thenAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            assertThat(request.toolSpecifications())
                    .extracting(ToolSpecification::name)
                    .containsExactly(toolName);
            if (chatCount.getAndIncrement() == 0) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("mcp-call-1")
                                .name(toolName)
                                .arguments("{\"query\":\"littlehorse\"}")
                                .build()))
                        .build();
            }
            ToolExecutionResultMessage result = assertInstanceOf(
                    ToolExecutionResultMessage.class, request.messages().getLast());
            assertThat(result.text()).isEqualTo("MCP search result");
            return ChatResponse.builder().aiMessage(AiMessage.from("Found it")).build();
        });
        TestWorkerContext context = new TestWorkerContext();

        String response =
                agentExecutor(model, Optional.empty(), toolsManager).textToText("Search", context);

        assertThat(response).isEqualTo("Found it");
        assertThat(chatCount).hasValue(2);
        assertThat(context.checkpointCount).isEqualTo(4);
        ToolExecutionResultMessageStruct checkpoint = toolResultCheckpoint(context, 2);
        assertThat(checkpoint.getId()).isEqualTo("mcp-call-1");
        assertThat(checkpoint.getToolName()).isEqualTo(toolName);
        assertThat(checkpoint.getContents()[0].getText()).isEqualTo("MCP search result");
        assertThat(checkpoint.getIsError()).isFalse();
        verify(mcpClient).listTools();
        verify(mcpClient)
                .executeTool(any(ToolExecutionRequest.class), any(InvocationContext.class));
    }

    @Test
    void stopsAfterTheConfiguredMaximumToolRounds() {
        String toolName = "remote_search";
        ToolSpecification specification = ToolSpecification.builder()
                .name(toolName)
                .parameters(JsonObjectSchema.builder().build())
                .build();
        AtomicInteger toolExecutionCount = new AtomicInteger();
        McpClient mcpClient = mcpClient("remote", List.of(specification), ignored -> {
            toolExecutionCount.incrementAndGet();
            return ToolExecutionResult.builder().resultText("result").build();
        });
        ToolsManager toolsManager =
                new ToolsManager(McpToolProvider.builder().mcpClients(mcpClient).build());
        AtomicInteger chatCount = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class))).thenAnswer(ignored -> ChatResponse.builder()
                .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                        .id("tool-call-" + chatCount.incrementAndGet())
                        .name(toolName)
                        .arguments("{}")
                        .build()))
                .build());
        TestWorkerContext context = new TestWorkerContext();

        assertThatThrownBy(() -> agentExecutor(model, Optional.empty(), toolsManager, 2)
                        .textToText("Search", context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The model exceeded the limit of 2 tool rounds");

        assertThat(chatCount).hasValue(2);
        assertThat(toolExecutionCount).hasValue(2);
        assertThat(context.checkpointCount).isEqualTo(5);
    }

    @Test
    void restoresEveryChatAndToolOutputWithoutRepeatingWork() {
        ToolExecutionRequest toolRequest = checkpointedToolRequest();
        TestWorkerContext context = new TestWorkerContext(
                UserMessageStruct.from(UserMessage.from("Hello")),
                chatResponseStruct(AiMessage.from(toolRequest)),
                toolResultStruct(toolRequest, "Restored tool result"),
                chatResponseStruct(AiMessage.from("Restored final response")));
        ChatModel model = mock(ChatModel.class);

        String response = agentExecutor(model, Optional.empty(), emptyToolsManager())
                .textToText("Hello", context);

        assertThat(response).isEqualTo("Restored final response");
        assertThat(context.checkpointCount).isEqualTo(4);
        assertThat(context.restoredValues).isEmpty();
        verifyNoInteractions(model);
    }

    @Test
    void rejectsToolResultsWithUnsupportedNonTextContent() {
        String toolName = "image_tool";
        ToolSpecification specification = ToolSpecification.builder()
                .name(toolName)
                .parameters(JsonObjectSchema.builder().build())
                .build();
        McpClient mcpClient =
                mcpClient("remote", List.of(specification), ignored -> ToolExecutionResult.builder()
                        .resultContents(
                                List.of(ImageContent.from("https://example.test/image.png")))
                        .build());
        ToolsManager toolsManager =
                new ToolsManager(McpToolProvider.builder().mcpClients(mcpClient).build());
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("image-call")
                                .name(toolName)
                                .arguments("{}")
                                .build()))
                        .build());

        assertThatThrownBy(() -> agentExecutor(model, Optional.empty(), toolsManager)
                        .textToText("Create an image", new TestWorkerContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returned unsupported non-text content");
    }

    @Test
    void usesTheConfiguredOutputStructDefAsTheResponseSchema() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"name\":\"Ada\"}"))
                        .build());
        StructDef structDef = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("agent-output"))
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields(
                                "name",
                                StructFieldDef.newBuilder()
                                        .setFieldType(TypeDefinition.newBuilder()
                                                .setPrimitiveType(VariableType.STR))
                                        .build()))
                .build();
        AgentExecutor executor = new AgentExecutor(
                model,
                mockAgentConfiguration(10),
                Optional.empty(),
                emptyToolsManager(),
                structDefResolver(structDef),
                userMessageTemplateRenderer("Convert {{struct}}"));

        InlineStruct response =
                executor.structToStruct(InlineStruct.getDefaultInstance(), new TestWorkerContext());

        assertThat(response.getFieldsOrThrow("name").getValue().getStr()).isEqualTo("Ada");
        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(requestCaptor.capture());
        UserMessage userMessage = assertInstanceOf(
                UserMessage.class, requestCaptor.getValue().messages().getFirst());
        assertThat(userMessage.singleText()).isEqualTo("Convert {}");
        assertThat(requestCaptor.getValue().responseFormat().type())
                .isEqualTo(ResponseFormatType.JSON);
        assertThat(requestCaptor.getValue().responseFormat().jsonSchema().name())
                .isEqualTo("agent-output");
    }

    private static ToolExecutionResultMessageStruct toolResultCheckpoint(
            TestWorkerContext context, int index) {
        return assertInstanceOf(
                ToolExecutionResultMessageStruct.class, context.checkpoints.get(index));
    }

    private static StructField field(VariableValue.Builder value) {
        return StructField.newBuilder().setValue(value).build();
    }

    private static ToolExecutionResultMessageStruct toolResultStruct(
            ToolExecutionRequest request, String result) {
        return ToolExecutionResultMessageStruct.from(
                ToolExecutionResultMessage.from(request, result));
    }

    private static ChatResponseStruct chatResponseStruct(AiMessage aiMessage) {
        return ChatResponseStruct.from(
                ChatResponse.builder().aiMessage(aiMessage).build());
    }

    private static ToolExecutionRequest checkpointedToolRequest() {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name("checkpointed_tool")
                .arguments("{}")
                .build();
    }

    private static ToolsManager emptyToolsManager() {
        return new ToolsManager(McpToolProvider.builder().mcpClients(List.of()).build());
    }

    private static AgentExecutor agentExecutor(
            ChatModel model, Optional<SystemMessage> systemMessage, ToolsManager toolsManager) {
        return agentExecutor(model, systemMessage, toolsManager, 10);
    }

    private static AgentExecutor agentExecutor(
            ChatModel model,
            Optional<SystemMessage> systemMessage,
            ToolsManager toolsManager,
            int maxToolRounds) {
        return new AgentExecutor(
                model,
                mockAgentConfiguration(maxToolRounds),
                systemMessage,
                toolsManager,
                unavailableStructDefResolver(),
                userMessageTemplateRenderer("{{struct}}"));
    }

    private static AgentConfiguration mockAgentConfiguration(int maxToolRounds) {
        AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
        when(agentConfiguration.maxToolRounds()).thenReturn(maxToolRounds);
        return agentConfiguration;
    }

    @SuppressWarnings("unchecked")
    private static Instance<StructDefResolver> unavailableStructDefResolver() {
        return mock(Instance.class);
    }

    private static Instance<StructDefResolver> structDefResolver(StructDef structDef) {
        Instance<StructDefResolver> instance = unavailableStructDefResolver();
        StructDefResolver resolver = mock(StructDefResolver.class);
        when(resolver.resolve())
                .thenReturn(new StructDefSnapshot(
                        structDef,
                        Map.of(structDef.getId(), structDef),
                        JsonSchema.builder()
                                .name(structDef.getId().getName())
                                .rootElement(JsonObjectSchema.builder().build())
                                .build()));
        when(instance.get()).thenReturn(resolver);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static Instance<UserMessageTemplateRenderer> userMessageTemplateRenderer(
            String template) {
        Instance<UserMessageTemplateRenderer> instance = mock(Instance.class);
        AgentConfiguration configuration = mock(AgentConfiguration.class);
        when(configuration.userMessageTemplate()).thenReturn(Optional.of(template));
        UserMessageTemplateRenderer renderer = new UserMessageTemplateRenderer(configuration);
        when(instance.get()).thenReturn(renderer);
        return instance;
    }

    private static McpClient mcpClient(
            String key,
            List<ToolSpecification> tools,
            Function<ToolExecutionRequest, ToolExecutionResult> execution) {
        McpClient client = mock(McpClient.class);
        when(client.key()).thenReturn(key);
        when(client.listTools()).thenReturn(tools);
        when(client.executeTool(any(ToolExecutionRequest.class), any(InvocationContext.class)))
                .thenAnswer(invocation -> execution.apply(invocation.getArgument(0)));
        return client;
    }

    private static final class TestWorkerContext extends WorkerContext {

        private final Deque<Object> restoredValues;
        private final List<Object> checkpoints = new ArrayList<>();
        private int checkpointCount;

        private TestWorkerContext(Object... restoredValues) {
            super(ScheduledTask.getDefaultInstance(), null, null);
            this.restoredValues = new ArrayDeque<>(List.of(restoredValues));
        }

        @Override
        public <T> T executeAndCheckpoint(
                CheckpointableFunction<T> checkpointableFunction, Class<T> clazz) {
            checkpointCount++;
            if (!restoredValues.isEmpty()) {
                Object restored = restoredValues.removeFirst();
                assertThat(restored).isExactlyInstanceOf(clazz);
                checkpoints.add(restored);
                return clazz.cast(restored);
            }
            T result = checkpointableFunction.run(new CheckpointContext());
            assertThat(result).isInstanceOf(clazz);
            checkpoints.add(result);
            return result;
        }
    }
}

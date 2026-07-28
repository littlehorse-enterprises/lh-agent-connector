package io.littlehorse.connector.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import io.quarkus.runtime.Startup;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

/**
 * {@link ChatModelListener} that persists every LLM request/response into PostgreSQL. Quarkus
 * LangChain4j auto-registers any {@code ChatModelListener} CDI bean on every chat model, so this
 * captures the full agent context (messages, tools, token usage) for all agents.
 *
 * <p>Each row is correlated with the LittleHorse {@code TaskRunId} exposed through {@link LlmContext}
 * when a task invokes an assistant.
 */
@Startup
@ApplicationScoped
public class PostgresChatModelLogger implements ChatModelListener {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresChatModelLogger.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public PostgresChatModelLogger(final DataSource dataSource, final ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void createTable() {
        final String ddl = "CREATE TABLE IF NOT EXISTS llm_interaction ("
                + "id BIGSERIAL PRIMARY KEY, "
                + "created_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
                + "task_run_id TEXT, "
                + "model TEXT, "
                + "request JSONB NOT NULL, "
                + "response JSONB, "
                + "prompt_tokens INTEGER, "
                + "completion_tokens INTEGER, "
                + "total_tokens INTEGER, "
                + "error TEXT)";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(ddl)) {
            statement.execute();
        } catch (final SQLException e) {
            throw new IllegalStateException("Could not initialize the llm_interaction table", e);
        }
    }

    @Override
    public void onResponse(final ChatModelResponseContext context) {
        final ChatRequest request = context.chatRequest();
        final ChatResponse response = context.chatResponse();
        final TokenUsage usage = response.tokenUsage();
        final String model = response.modelName() != null ? response.modelName() : request.modelName();
        final String sql = "INSERT INTO llm_interaction "
                + "(task_run_id, model, request, response, prompt_tokens, completion_tokens, total_tokens) "
                + "VALUES (?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, LlmContext.currentTaskRunId());
            statement.setString(2, model);
            statement.setString(3, requestJson(request));
            statement.setString(4, responseJson(response));
            setNullableInt(statement, 5, usage == null ? null : usage.inputTokenCount());
            setNullableInt(statement, 6, usage == null ? null : usage.outputTokenCount());
            setNullableInt(statement, 7, usage == null ? null : usage.totalTokenCount());
            statement.executeUpdate();
        } catch (final SQLException e) {
            // Never fail the agent because logging failed; just report it.
            LOG.error("Could not persist LLM interaction", e);
        }
    }

    @Override
    public void onError(final ChatModelErrorContext context) {
        final ChatRequest request = context.chatRequest();
        final String sql = "INSERT INTO llm_interaction (task_run_id, model, request, error) "
                + "VALUES (?, ?, ?::jsonb, ?)";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, LlmContext.currentTaskRunId());
            statement.setString(2, request.modelName());
            statement.setString(3, requestJson(request));
            statement.setString(4, String.valueOf(context.error()));
            statement.executeUpdate();
        } catch (final SQLException e) {
            LOG.error("Could not persist failed LLM interaction", e);
        }
    }

    private String requestJson(final ChatRequest request) {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.modelName());
        root.put("temperature", request.temperature());
        root.put("topP", request.topP());
        root.put("frequencyPenalty", request.frequencyPenalty());
        root.put("presencePenalty", request.presencePenalty());
        root.put("maxOutputTokens", request.maxOutputTokens());
        try {
            root.set(
                    "messages",
                    objectMapper.readTree(ChatMessageSerializer.messagesToJson(request.messages())));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize chat request messages", e);
        }
        final List<ToolSpecification> tools = request.toolSpecifications();
        if (tools != null && !tools.isEmpty()) {
            final var toolsNode = root.putArray("tools");
            for (final ToolSpecification tool : tools) {
                toolsNode.add(tool.name());
            }
        }
        return writeAsString(root);
    }

    private String responseJson(final ChatResponse response) {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("id", response.id());
        root.put("model", response.modelName());
        root.put("finishReason", response.finishReason() == null ? null : response.finishReason().name());
        try {
            root.set(
                    "aiMessage",
                    objectMapper.readTree(
                            ChatMessageSerializer.messagesToJson(List.of(response.aiMessage()))));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize chat response message", e);
        }
        final TokenUsage usage = response.tokenUsage();
        if (usage != null) {
            final ObjectNode usageNode = root.putObject("tokenUsage");
            usageNode.put("inputTokenCount", usage.inputTokenCount());
            usageNode.put("outputTokenCount", usage.outputTokenCount());
            usageNode.put("totalTokenCount", usage.totalTokenCount());
        }
        return writeAsString(root);
    }

    private String writeAsString(final ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize LLM interaction JSON", e);
        }
    }

    private static void setNullableInt(
            final PreparedStatement statement, final int index, final Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}

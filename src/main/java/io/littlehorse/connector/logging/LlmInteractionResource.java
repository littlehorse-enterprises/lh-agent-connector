package io.littlehorse.connector.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/** REST endpoint that returns the LLM interactions logged for a given LittleHorse {@code TaskRunId}. */
@Path("/llm-interactions")
@Produces(MediaType.APPLICATION_JSON)
public class LlmInteractionResource {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public LlmInteractionResource(final DataSource dataSource, final ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @GET
    @Path("/{taskRunId:.+}")
    public List<LlmInteractionRecord> byTaskRunId(@PathParam("taskRunId") final String taskRunId) {
        if (taskRunId == null || taskRunId.isBlank()) {
            throw new WebApplicationException(
                    "The 'taskRunId' path parameter is required.", Response.Status.BAD_REQUEST);
        }
        final String sql = "SELECT id, created_at, task_run_id, model, request, response, "
                + "prompt_tokens, completion_tokens, total_tokens, error "
                + "FROM llm_interaction WHERE task_run_id = ? ORDER BY id";
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskRunId);
            try (final ResultSet resultSet = statement.executeQuery()) {
                final List<LlmInteractionRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(new LlmInteractionRecord(
                            resultSet.getLong("id"),
                            resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                            resultSet.getString("task_run_id"),
                            resultSet.getString("model"),
                            toJson(resultSet.getString("request")),
                            toJson(resultSet.getString("response")),
                            nullableInt(resultSet, "prompt_tokens"),
                            nullableInt(resultSet, "completion_tokens"),
                            nullableInt(resultSet, "total_tokens"),
                            resultSet.getString("error")));
                }
                return records;
            }
        } catch (final SQLException e) {
            throw new WebApplicationException(
                    "Could not read logged LLM interactions.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonNode toJson(final String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (final JsonProcessingException e) {
            throw new WebApplicationException(
                    "Stored LLM interaction JSON is malformed.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private static Integer nullableInt(final ResultSet resultSet, final String column)
            throws SQLException {
        final int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}

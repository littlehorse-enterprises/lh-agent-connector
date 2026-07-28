package io.littlehorse.connector.logging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

/** A single logged LLM interaction returned by {@link LlmInteractionResource}. */
public record LlmInteractionRecord(
        long id,
        OffsetDateTime createdAt,
        String taskRunId,
        String model,
        JsonNode request,
        JsonNode response,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String error) {}

package io.littlehorse.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.mcp.client.McpToolResultExtractor;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.ArrayList;
import java.util.List;

/** Normalizes MCP text and embedded text resources for the agent's text-only tool messages. */
final class TextResourceToolResultExtractor implements McpToolResultExtractor {

    @Override
    public ToolExecutionResult extract(JsonNode content, boolean isError) {
        if (content == null || !content.isArray()) {
            throw new IllegalArgumentException("MCP tool content must be an array");
        }
        List<String> fragments = new ArrayList<>();
        for (JsonNode item : content) {
            String type = requiredText(item, "type");
            switch (type) {
                case "text" -> fragments.add(requiredText(item, "text"));
                case "resource" -> {
                    JsonNode resource = item.path("resource");
                    if (resource.has("blob")) {
                        throw new IllegalArgumentException(
                                "MCP tool returned a binary resource; only embedded text resources are supported");
                    }
                    String uri = requiredText(resource, "uri");
                    String text = requiredText(resource, "text");
                    String mimeType = resource.has("mimeType")
                            ? " (" + requiredText(resource, "mimeType") + ")"
                            : "";
                    fragments.add("Resource: " + uri + mimeType + "\n" + text);
                }
                default ->
                    throw new IllegalArgumentException("Unsupported MCP tool content type: " + type
                            + "; only text and embedded text resources are supported");
            }
        }
        return ToolExecutionResult.builder()
                .isError(isError)
                .resultText(String.join("\n", fragments))
                .build();
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(
                    "MCP tool content requires a string '" + field + "'");
        }
        return value.textValue();
    }
}

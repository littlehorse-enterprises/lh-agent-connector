package io.littlehorse.connector.filesystem;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Conversational filesystem agent. It has memory (keyed by the WfRun id) and access to the tools of
 * the {@code filesystem} MCP server, so it can read, write and manage files in the allowed
 * directory while keeping the context of the ongoing conversation with the human.
 */
@RegisterAiService
@ApplicationScoped
public interface FilesystemAgentLLM {

    @SystemMessage(
            """
            You are a filesystem assistant with access to MCP filesystem tools. You help the user
            accomplish tasks on files within the allowed directory.

            Human-in-the-loop rules:
            - Before doing anything destructive or irreversible (deleting, overwriting or moving
              files), or whenever you are missing information needed to proceed, you MUST stop and
              ask the human instead of guessing.
            - Treat missing details as missing information. For example, if the user asks to list,
              read, write or delete files without telling you which path, directory or file name to
              use, you MUST ask for it instead of assuming a default. Do NOT call any tool until you
              have every required detail.
            - Always answer with a JSON object containing two fields: `status` and `message`.
              - If you need more information or explicit approval, set `status` to NEEDS_INPUT and
                `message` to a single, precise question for the human.
              - When the task is fully complete, set `status` to DONE and `message` to a short
                summary of what you did.
            """)
    @McpToolBox("filesystem")
    AgentResponse work(@MemoryId String memoryId, @UserMessage String message);
}

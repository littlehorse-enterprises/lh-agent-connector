package io.littlehorse.connector.filesystem;

/**
 * Structured turn returned by the filesystem agent.
 *
 * @param status whether the agent finished or needs the human to answer first
 * @param message the final summary when {@code DONE}, or the question for the human when
 *     {@code NEEDS_INPUT}
 */
public record AgentResponse(AgentStatus status, String message) {

    /** Whether the agent finished the task or is waiting on the human. */
    public enum AgentStatus {
        NEEDS_INPUT,
        DONE
    }
}

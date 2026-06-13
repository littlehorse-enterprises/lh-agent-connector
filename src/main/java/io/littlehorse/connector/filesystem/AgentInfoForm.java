package io.littlehorse.connector.filesystem;

import io.littlehorse.quarkus.task.LHUserTaskForm;
import io.littlehorse.sdk.usertask.annotations.UserTaskField;

/**
 * User task form a human fills in to answer the filesystem agent's question or grant approval. Its
 * single field becomes the {@code provide-agent-info} UserTaskDef. The agent's question is shown to
 * the human in the user task notes.
 */
@LHUserTaskForm("provide-agent-info")
public class AgentInfoForm {

    @UserTaskField(
            displayName = "Answer",
            description = "Your answer or approval for the agent's question.",
            required = true)
    public String answer;
}

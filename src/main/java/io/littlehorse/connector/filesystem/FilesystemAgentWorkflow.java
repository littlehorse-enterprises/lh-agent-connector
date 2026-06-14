package io.littlehorse.connector.filesystem;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.UserTaskOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Dev-only conversational workflow: a filesystem agent (with MCP file tools) works on a task and
 * keeps a back-and-forth conversation with a human through LittleHorse {@code UserTask}s.
 *
 * <p>Each turn the agent either finishes the task or, when it needs more information or approval,
 * asks a question. A question pauses the workflow on a {@code UserTask}; the human's answer is fed
 * back to the agent (which remembers the whole conversation via its WfRun-scoped memory) and the
 * loop repeats until the agent is done.
 *
 * <p>Run it with lhctl, e.g.:
 *
 * <pre>
 * lhctl run filesystem-workflow task "Delete every .log file in the workspace directory."
 *
 * lhctl run filesystem-workflow task "Create a notes.txt file, but ask me what to write in it."
 * </pre>
 *
 * <p>Whenever the agent asks something, complete the {@code provide-agent-info} UserTask (e.g. from
 * the dashboard) to resume the conversation.
 */
@IfBuildProfile("dev")
@LHWorkflow("filesystem-workflow")
public class FilesystemAgentWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(final WorkflowThread wf) {
        final WfRunVariable task = wf.declareStr("task").required();
        final WfRunVariable agentInput = wf.declareStr("agent-input");
        final WfRunVariable agentResult = wf.declareJsonObj("agent-result");
        final WfRunVariable status = wf.declareStr("status").withDefault("NEEDS_INPUT");
        final WfRunVariable message = wf.declareStr("message");
        final WfRunVariable userTaskForm = wf.declareJsonObj("answer-form");
        final WfRunVariable humanAnswer = wf.declareStr("human-answer");
        final WfRunVariable finalAnswer = wf.declareStr("final-answer");

        // The first turn sends the original task; later turns send the human's answer.
        agentInput.assign(task);

        // do-while: run the agent at least once, then keep looping while it needs human input.
        wf.doWhile(status.isEqualTo("NEEDS_INPUT"), loop -> {
            final NodeOutput turn = loop.execute("run-filesystem-agent", agentInput);
            agentResult.assign(turn);
            status.assign(agentResult.jsonPath("$.status"));
            message.assign(agentResult.jsonPath("$.message"));

            loop.doIf(status.isEqualTo("NEEDS_INPUT"), needsInput -> {
                        // Human-in-the-loop: pause until a human answers the agent's question.
                        final UserTaskOutput userTask = needsInput
                                .assignUserTask("provide-agent-info", null, "agent-operators")
                                .withNotes(message);
                        userTaskForm.assign(userTask);
                        humanAnswer.assign(userTaskForm.jsonPath("$.answer"));
                        // Feed the answer back to the agent on the next turn.
                        agentInput.assign(humanAnswer);
                    })
                    .doElse(done -> {
                        finalAnswer.assign(message);
                    });
        });
    }
}

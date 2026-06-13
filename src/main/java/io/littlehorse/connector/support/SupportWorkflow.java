package io.littlehorse.connector.support;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.UserTaskOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Dev-only support workflow that combines an LLM with human-in-the-loop.
 *
 * <p>A customer submits a ticket in natural language. The LLM classifies it: plain feedback is just
 * logged, while an actionable support request pauses the workflow on a LittleHorse {@code UserTask}
 * so a human agent can resolve it. The agent's resolution notes are then handed back to the LLM to
 * draft a polished customer reply.
 *
 * <p>Run it with lhctl, e.g.:
 *
 * <pre>
 * lhctl run support-workflow ticket "Just wanted to say your new dashboard looks great, keep it up."
 *
 * lhctl run support-workflow ticket "I was charged twice for my subscription this month, please help."
 * </pre>
 *
 * <p>For a support request, complete the resulting UserTask (e.g. from the dashboard) to resume the
 * workflow and produce the drafted reply.
 */
@IfBuildProfile("dev")
@LHWorkflow("support-workflow")
public class SupportWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(final WorkflowThread wf) {
        final WfRunVariable ticket = wf.declareStr("ticket").required();
        final WfRunVariable result = wf.declareJsonObj("classification");
        final WfRunVariable type = wf.declareStr("type");
        final WfRunVariable summary = wf.declareStr("summary");
        final WfRunVariable resolutionForm = wf.declareJsonObj("resolution-form");
        final WfRunVariable resolution = wf.declareStr("resolution");
        final WfRunVariable reply = wf.declareStr("reply");

        final NodeOutput classification = wf.execute("classify-support-ticket", ticket);
        result.assign(classification);
        type.assign(result.jsonPath("$.type"));
        summary.assign(result.jsonPath("$.summary"));

        wf.doIf(type.isEqualTo("FEEDBACK"), feedback -> {
                    feedback.execute("log-feedback", summary);
                })
                .doElse(support -> {
                    // Human-in-the-loop: pause the WfRun until a support agent completes the form.
                    final UserTaskOutput userTask = support.assignUserTask(
                                    "resolve-support-ticket", null, "support-team")
                            .withNotes(summary);
                    resolutionForm.assign(userTask);
                    resolution.assign(resolutionForm.jsonPath("$.resolution"));

                    // Hand the human's resolution back to the LLM to draft the customer reply.
                    final NodeOutput draftedReply =
                            support.execute("draft-support-reply", ticket, resolution);
                    reply.assign(draftedReply);
                });
    }
}

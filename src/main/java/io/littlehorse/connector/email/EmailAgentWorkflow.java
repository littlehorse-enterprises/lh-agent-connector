package io.littlehorse.connector.email;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Dev-only workflow that reads an email, classifies it, and sends a Slack notification when the
 * email is a job opportunity.
 *
 * <p>Run it with lhctl, e.g.:
 *
 * <pre>
 * lhctl run email-agent-workflow email "
 * Subject: Software Engineer position at Acme Corp
 *
 * Hi, we came across your profile and would love to talk about a Senior Backend
 * Engineer role at Acme Corp. The position is remote and the salary range is
 * competitive. Are you available for a quick call this week?
 * "
 *
 * lhctl run email-agent-workflow email "
 * Subject: You WON a FREE iPhone.
 *
 * Congratulations. Click this link http://totally-legit.example to claim your
 * free prize now before it expires! Limited time only.
 * "
 * </pre>
 */
@IfBuildProfile("dev")
@LHWorkflow("email-agent-workflow")
public class EmailAgentWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(final WorkflowThread wf) {
        final WfRunVariable email = wf.declareStr("email").required();

        final NodeOutput classification = wf.execute("read-email", email);
        final WfRunVariable result = wf.declareJsonObj("classification");
        result.assign(classification);

        final WfRunVariable type = wf.declareStr("type");
        type.assign(result.jsonPath("$.type"));

        final WfRunVariable subject = wf.declareStr("subject");
        subject.assign(result.jsonPath("$.subject"));

        wf.doIf(type.isEqualTo("JOB_OPPORTUNITY"), handler -> {
            handler.execute(
                    "saddle-bag-slack-post-message",
                    "saddle-bag-test",
                    wf.format("Email alert [{0}]: {1}", type, subject));
        });
    }
}

package io.littlehorse.connector.general;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Dev-only workflow that sends a prompt to the LLM and exposes its response.
 *
 * <p>Run it with lhctl, e.g.:
 *
 * <pre>
 * lhctl run agent-workflow prompt "List all star wars movies"
 * </pre>
 */
@IfBuildProfile("dev")
@LHWorkflow("agent-workflow")
public class GeneralPurposesWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(final WorkflowThread wf) {
        final WfRunVariable prompt = wf.declareStr("prompt").required();
        final NodeOutput responsePrompt = wf.execute("ask-llm", prompt);
        final WfRunVariable answer = wf.declareStr("answer");
        answer.assign(responsePrompt);

        final NodeOutput responseTopic = wf.execute("ask-llm", "What are we talking about in this session?");
        final WfRunVariable topic = wf.declareStr("topic");
        topic.assign(responseTopic);
    }
}

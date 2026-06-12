package io.littlehorse.connector.workflow;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

@IfBuildProfile("dev")
@LHWorkflow("agent-workflow")
public class AgentWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(WorkflowThread wf) {
        WfRunVariable prompt = wf.declareStr("prompt").required();
        NodeOutput response = wf.execute("ask-llm", prompt);
        WfRunVariable answer = wf.declareStr("answer").asPublic();
        answer.assign(response);
    }
}

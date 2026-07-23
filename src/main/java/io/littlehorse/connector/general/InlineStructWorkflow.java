package io.littlehorse.connector.general;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.NodeOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;
import io.quarkus.arc.profile.IfBuildProfile;

/** Dev-only workflow demonstrating a registered StructDef consumed as a raw InlineStruct. */
@IfBuildProfile("dev")
@LHWorkflow("ask-llm-with-inline-struct-workflow")
public class InlineStructWorkflow implements LHWorkflowDefinition {

    @Override
    public void define(final WorkflowThread wf) {
        final WfRunVariable structuredPrompt =
                wf.declareStruct("structured-prompt", StructuredPrompt.class).required();

        final NodeOutput response = wf.execute("ask-llm-with-inline-struct", structuredPrompt);
        final WfRunVariable answer = wf.declareStr("answer");
        answer.assign(response);
    }
}

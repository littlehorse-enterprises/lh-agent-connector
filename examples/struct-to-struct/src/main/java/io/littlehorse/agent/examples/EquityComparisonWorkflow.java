package io.littlehorse.agent.examples;

import io.littlehorse.quarkus.workflow.LHWorkflow;
import io.littlehorse.quarkus.workflow.LHWorkflowDefinition;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.WorkflowThread;

@LHWorkflow(value = EquityComparisonWorkflow.WORKFLOW_NAME, defaultTaskTimeout = "300")
public class EquityComparisonWorkflow implements LHWorkflowDefinition {

    public static final String WORKFLOW_NAME = "equity-comparison-example";
    public static final String AGENT_TASK_NAME = "struct-to-struct-agent";
    public static final String INPUT_VARIABLE = "input";
    public static final String OUTPUT_VARIABLE = "output";

    @Override
    public void define(WorkflowThread wf) {
        WfRunVariable input =
                wf.declareStruct(INPUT_VARIABLE, EquityComparisonRequest.class).required();
        WfRunVariable output = wf.declareStruct(OUTPUT_VARIABLE, EquityComparisonReport.class);

        output.assign(wf.execute(AGENT_TASK_NAME, input));
    }
}

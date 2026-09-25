package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

@LHStructDef(
        value = "equity-comparison-request",
        description = "Two exact ticker symbols and a historical date window to compare.")
public class EquityComparisonRequest {

    @LHStructField(description = "Caller-supplied identifier copied into the report.")
    private String requestId;

    @LHStructField(
            description =
                    "Exact Yahoo Finance ticker symbol of the company being researched, such as AAPL.")
    private String subjectSymbol;

    @LHStructField(
            description = "Exact Yahoo Finance ticker symbol used for comparison, such as MSFT.")
    private String benchmarkSymbol;

    @LHStructField(description = "Historical date range, embedded as an inline struct.")
    private AnalysisWindow window;

    public EquityComparisonRequest() {}

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSubjectSymbol() {
        return subjectSymbol;
    }

    public void setSubjectSymbol(String subjectSymbol) {
        this.subjectSymbol = subjectSymbol;
    }

    public String getBenchmarkSymbol() {
        return benchmarkSymbol;
    }

    public void setBenchmarkSymbol(String benchmarkSymbol) {
        this.benchmarkSymbol = benchmarkSymbol;
    }

    public AnalysisWindow getWindow() {
        return window;
    }

    public void setWindow(AnalysisWindow window) {
        this.window = window;
    }
}

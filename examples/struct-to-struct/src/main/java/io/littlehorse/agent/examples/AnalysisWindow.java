package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructField;

/** An unannotated POJO becomes an inline struct inside EquityComparisonRequest. */
public class AnalysisWindow {

    @LHStructField(description = "Inclusive start date for get_chart, formatted YYYY-MM-DD.")
    private String fromDate;

    @LHStructField(description = "Exclusive end date for get_chart, formatted YYYY-MM-DD.")
    private String toDate;

    public AnalysisWindow() {}

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }
}

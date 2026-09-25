package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructField;

/** An unannotated POJO becomes an inline struct inside EquityComparisonReport. */
public class ComparisonSummary {

    @LHStructField(
            description =
                    "Symbol with the higher calculated window percentChange; null if either result is unavailable or tied.",
            isNullable = true)
    private String higherWindowReturnSymbol;

    @LHStructField(
            description =
                    "One factual sentence comparing the two window returns, or explaining why they cannot be compared.")
    private String explanation;

    public ComparisonSummary() {}

    public String getHigherWindowReturnSymbol() {
        return higherWindowReturnSymbol;
    }

    public void setHigherWindowReturnSymbol(String higherWindowReturnSymbol) {
        this.higherWindowReturnSymbol = higherWindowReturnSymbol;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

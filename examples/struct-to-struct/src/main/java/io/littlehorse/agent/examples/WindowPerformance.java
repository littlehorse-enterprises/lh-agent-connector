package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

@LHStructDef(
        value = "window-performance",
        description = "Historical closing-price movement for one ticker in the requested window.")
public class WindowPerformance {

    @LHStructField(description = "Exact ticker symbol from the input request.")
    private String symbol;

    @LHStructField(
            description =
                    "Date of the first returned daily candle in the requested window; null if no candles.",
            isNullable = true)
    private String firstTradingDate;

    @LHStructField(
            description = "First returned daily close from get_chart; null if unavailable.",
            isNullable = true)
    private Double firstClose;

    @LHStructField(
            description =
                    "Date of the last returned daily candle in the requested window; null if no candles.",
            isNullable = true)
    private String lastTradingDate;

    @LHStructField(
            description = "Last returned daily close from get_chart; null if unavailable.",
            isNullable = true)
    private Double lastClose;

    @LHStructField(
            description =
                    "100 * (lastClose - firstClose) / firstClose, in percent; null if closes are missing or firstClose is zero.",
            isNullable = true)
    private Double percentChange;

    public WindowPerformance() {}

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getFirstTradingDate() {
        return firstTradingDate;
    }

    public void setFirstTradingDate(String firstTradingDate) {
        this.firstTradingDate = firstTradingDate;
    }

    public Double getFirstClose() {
        return firstClose;
    }

    public void setFirstClose(Double firstClose) {
        this.firstClose = firstClose;
    }

    public String getLastTradingDate() {
        return lastTradingDate;
    }

    public void setLastTradingDate(String lastTradingDate) {
        this.lastTradingDate = lastTradingDate;
    }

    public Double getLastClose() {
        return lastClose;
    }

    public void setLastClose(Double lastClose) {
        this.lastClose = lastClose;
    }

    public Double getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }
}

package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

@LHStructDef(
        value = "instrument-snapshot",
        description = "One symbol's latest available Yahoo Finance quote.")
public class InstrumentSnapshot {

    @LHStructField(description = "Exact ticker symbol from the input request.")
    private String symbol;

    @LHStructField(
            description = "Company or instrument name from get_quote, if supplied.",
            isNullable = true)
    private String name;

    @LHStructField(
            description = "ISO currency code of the quoted price from get_quote, if supplied.",
            isNullable = true)
    private String currency;

    @LHStructField(
            description = "regularMarketPrice from get_quote; null if unavailable.",
            isNullable = true)
    private Double lastPrice;

    @LHStructField(
            description = "regularMarketPreviousClose from get_quote; null if unavailable.",
            isNullable = true)
    private Double previousClose;

    @LHStructField(
            description = "regularMarketTime from get_quote in ISO-8601 UTC; null if unavailable.",
            isNullable = true)
    private String observedAt;

    public InstrumentSnapshot() {}

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(String observedAt) {
        this.observedAt = observedAt;
    }
}

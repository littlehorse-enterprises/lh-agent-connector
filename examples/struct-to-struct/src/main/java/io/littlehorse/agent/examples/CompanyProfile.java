package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructField;

/** An unannotated POJO becomes an inline struct inside EquityComparisonReport. */
public class CompanyProfile {

    @LHStructField(
            description = "Sector from quote_summary assetProfile; null if unavailable.",
            isNullable = true)
    private String sector;

    @LHStructField(
            description = "Industry from quote_summary assetProfile; null if unavailable.",
            isNullable = true)
    private String industry;

    @LHStructField(
            description =
                    "Revenue growth decimal fraction from quote_summary financialData, such as 0.12 for 12%; null if unavailable.",
            isNullable = true)
    private Double revenueGrowth;

    public CompanyProfile() {}

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Double getRevenueGrowth() {
        return revenueGrowth;
    }

    public void setRevenueGrowth(Double revenueGrowth) {
        this.revenueGrowth = revenueGrowth;
    }
}

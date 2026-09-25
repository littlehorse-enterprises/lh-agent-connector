package io.littlehorse.agent.examples;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Map;

@LHStructDef(
        value = "equity-comparison-report",
        description =
                "Structured comparison of two tickers using Yahoo Finance quotes, charts, and company data.")
public class EquityComparisonReport {

    @LHStructField(description = "The identifier from the input request, copied exactly.")
    private String requestId;

    @LHStructField(
            description =
                    "Quote for the subject ticker; this field references the named instrument-snapshot StructDef.")
    private InstrumentSnapshot subject;

    @LHStructField(
            description =
                    "Quote for the benchmark ticker; reuses the instrument-snapshot StructDef.")
    private InstrumentSnapshot benchmark;

    @LHStructField(
            description = "Subject company profile and financial growth fields, embedded inline.")
    private CompanyProfile subjectProfile;

    @LHStructField(
            description =
                    "One historical performance entry per requested symbol; each array item references window-performance.")
    private WindowPerformance[] performance;

    @LHStructField(description = "Compact comparison conclusion, embedded inline.")
    private ComparisonSummary summary;

    @LHStructField(
            description =
                    "Map of report section names to Yahoo MCP tool names actually used, such as quotes to get_quote.")
    private Map<String, String> sourcesBySection;

    @LHStructField(
            description =
                    "Data gaps, failed tool calls, stale quotes, or other concrete limitations; empty if none.")
    private String[] warnings;

    public EquityComparisonReport() {}

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public InstrumentSnapshot getSubject() {
        return subject;
    }

    public void setSubject(InstrumentSnapshot subject) {
        this.subject = subject;
    }

    public InstrumentSnapshot getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(InstrumentSnapshot benchmark) {
        this.benchmark = benchmark;
    }

    public CompanyProfile getSubjectProfile() {
        return subjectProfile;
    }

    public void setSubjectProfile(CompanyProfile subjectProfile) {
        this.subjectProfile = subjectProfile;
    }

    public WindowPerformance[] getPerformance() {
        return performance;
    }

    public void setPerformance(WindowPerformance[] performance) {
        this.performance = performance;
    }

    public ComparisonSummary getSummary() {
        return summary;
    }

    public void setSummary(ComparisonSummary summary) {
        this.summary = summary;
    }

    public Map<String, String> getSourcesBySection() {
        return sourcesBySection;
    }

    public void setSourcesBySection(Map<String, String> sourcesBySection) {
        this.sourcesBySection = sourcesBySection;
    }

    public String[] getWarnings() {
        return warnings;
    }

    public void setWarnings(String[] warnings) {
        this.warnings = warnings;
    }
}

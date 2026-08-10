package io.littlehorse.agent.message;

import dev.langchain4j.model.output.TokenUsage;

import io.littlehorse.sdk.worker.LHStructDef;
import io.littlehorse.sdk.worker.LHStructField;

import java.util.Objects;

/** LittleHorse Struct representation of LangChain4j {@link TokenUsage}. */
@LHStructDef(value = "token-usage", description = "Token counts for an LLM response.")
public class TokenUsageStruct {

    @LHStructField(isNullable = true)
    private Integer inputTokenCount;

    @LHStructField(isNullable = true)
    private Integer outputTokenCount;

    @LHStructField(isNullable = true)
    private Integer totalTokenCount;

    public TokenUsageStruct() {}

    public Integer getInputTokenCount() {
        return inputTokenCount;
    }

    public void setInputTokenCount(Integer inputTokenCount) {
        this.inputTokenCount = inputTokenCount;
    }

    public Integer getOutputTokenCount() {
        return outputTokenCount;
    }

    public void setOutputTokenCount(Integer outputTokenCount) {
        this.outputTokenCount = outputTokenCount;
    }

    public Integer getTotalTokenCount() {
        return totalTokenCount;
    }

    public void setTotalTokenCount(Integer totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }

    public static TokenUsageStruct from(TokenUsage tokenUsage) {
        Objects.requireNonNull(tokenUsage);
        TokenUsageStruct struct = new TokenUsageStruct();
        struct.setInputTokenCount(tokenUsage.inputTokenCount());
        struct.setOutputTokenCount(tokenUsage.outputTokenCount());
        struct.setTotalTokenCount(tokenUsage.totalTokenCount());
        return struct;
    }

    public static TokenUsage toTokenUsage(TokenUsageStruct struct) {
        Objects.requireNonNull(struct);
        return new TokenUsage(
                struct.getInputTokenCount(),
                struct.getOutputTokenCount(),
                struct.getTotalTokenCount());
    }
}

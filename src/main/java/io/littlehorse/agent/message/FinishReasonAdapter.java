package io.littlehorse.agent.message;

import dev.langchain4j.model.output.FinishReason;

import io.littlehorse.sdk.common.adapter.LHStringAdapter;

import jakarta.inject.Singleton;

/** Maps LangChain4j's finish-reason enum to the LittleHorse {@code STR} type. */
@Singleton
public class FinishReasonAdapter implements LHStringAdapter<FinishReason> {

    @Override
    public String toString(FinishReason finishReason) {
        return finishReason.name();
    }

    @Override
    public FinishReason fromString(String value) {
        return FinishReason.valueOf(value);
    }

    @Override
    public Class<FinishReason> getTypeClass() {
        return FinishReason.class;
    }
}

package io.littlehorse.connector.general;

import io.littlehorse.sdk.worker.LHStructDef;

@LHStructDef("${lhc.general.structured-prompt.name}")
public class StructuredPrompt {

    private String prompt;
    private String context;

    public StructuredPrompt() {}

    public StructuredPrompt(final String prompt, final String context) {
        this.prompt = prompt;
        this.context = context;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
    }
}

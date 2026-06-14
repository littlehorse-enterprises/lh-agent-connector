package io.littlehorse.connector.email;

/** Classification result produced by the {@link EmailReaderLLM}. */
public record EmailClassification(EmailType type, String subject) {

    public enum EmailType {
        SPAM,
        SALES_OPPORTUNITY,
        NOT_IMPORTANT
    }
}

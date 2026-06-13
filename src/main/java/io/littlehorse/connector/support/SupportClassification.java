package io.littlehorse.connector.support;

/**
 * Result of classifying a customer support ticket.
 *
 * @param type a short summary the support agent can read at a glance
 * @param summary one of {@link SupportType}
 */
public record SupportClassification(SupportType type, String summary) {

    /** Whether the ticket is plain feedback or an actionable support request. */
    public enum SupportType {
        FEEDBACK,
        SUPPORT_REQUEST
    }
}

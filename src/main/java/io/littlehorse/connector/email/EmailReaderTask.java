package io.littlehorse.connector.email;

import dev.langchain4j.exception.NonRetriableException;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.common.exception.LHTaskException;
import io.littlehorse.sdk.worker.LHTaskMethod;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class EmailReaderTask {

    private static final Logger LOG = LoggerFactory.getLogger(EmailReaderTask.class);

    private final EmailReaderLLM emailReader;

    @Inject
    public EmailReaderTask(EmailReaderLLM emailReader) {
        this.emailReader = emailReader;
    }

    @LHTaskMethod(value = "read-email", description = "Classifies an email as SPAM, JOB_OPPORTUNITY or NOT_IMPORTANT.")
    public EmailClassification readEmail(String email) {
        LOG.info("Classifying email of length {}", email == null ? 0 : email.length());
        try {
            EmailClassification classification = emailReader.classify(email);
            LOG.info("Email classified as {}", classification.type());
            return classification;
        } catch (NonRetriableException e) {
            // Permanent failures (auth/empty credits, misconfiguration, invalid request): do not
            // retry. Throwing LHTaskException raises a business EXCEPTION instead of a retryable
            // TASK_FAILURE.
            LOG.error("Non-retriable LLM error while classifying email", e);
            throw new LHTaskException("llm-non-retriable", e.getMessage());
        }
        // RetriableException (timeouts, rate limits, server errors) and any other RuntimeException
        // propagate as a retryable TASK_FAILURE so LittleHorse retries the task.
    }
}

package io.littlehorse.connector.email;

import io.littlehorse.connector.logging.LlmContext;
import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class EmailReaderTask {

    private static final Logger LOG = LoggerFactory.getLogger(EmailReaderTask.class);

    private final EmailReaderLLM emailReader;

    @Inject
    public EmailReaderTask(final EmailReaderLLM emailReader) {
        this.emailReader = emailReader;
    }

    @LHTaskMethod(
            value = "read-email",
            description = "Classifies an email as SPAM, SALES_OPPORTUNITY or NOT_IMPORTANT.")
    public EmailClassification readEmail(final String email, final WorkerContext context) {
        LOG.info("Classifying email of length {}", email == null ? 0 : email.length());
        final EmailClassification classification =
                LlmContext.runLlmTask(context, () -> emailReader.classify(email));
        LOG.info("Email classified as {}", classification.type());
        return classification;
    }
}

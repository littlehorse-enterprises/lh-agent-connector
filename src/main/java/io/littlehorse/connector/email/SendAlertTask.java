package io.littlehorse.connector.email;

import io.littlehorse.quarkus.task.LHTask;
import io.littlehorse.sdk.worker.LHTaskMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LHTask
public class SendAlertTask {

    private static final Logger LOG = LoggerFactory.getLogger(SendAlertTask.class);

    @LHTaskMethod(
            value = "send-alert",
            description = "Dummy alert sender that logs the alert message.")
    public String sendAlert(final String message) {
        LOG.info("ALERT: {}", message);
        return message;
    }
}

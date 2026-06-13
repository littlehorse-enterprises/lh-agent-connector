package io.littlehorse.connector.support;

import io.littlehorse.quarkus.task.LHUserTaskForm;
import io.littlehorse.sdk.usertask.annotations.UserTaskField;

/**
 * User task form a human support agent fills in to resolve a support request. Its public fields
 * annotated with {@link UserTaskField} become the fields of the {@code resolve-support-ticket}
 * UserTaskDef.
 */
@LHUserTaskForm("resolve-support-ticket")
public class SupportTicketForm {

    @UserTaskField(
            displayName = "Resolution",
            description = "How the support request was resolved for the customer.",
            required = true)
    public String resolution;
}

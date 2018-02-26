package org.apache.james.webadmin.service;

import com.google.common.base.Throwables;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;

import javax.mail.MessagingException;
import java.util.Optional;
import java.util.function.Supplier;

public class ClearMailQueueTask implements Task {

    public static final String TYPE = "clearMailQueue";

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final String queueName;
        private final Supplier<Long> countSupplier;
        private final long initialCount;

        public AdditionalInformation(String queueName, Supplier<Long> countSupplier) {
            this.queueName = queueName;
            this.initialCount = countSupplier.get();
            this.countSupplier = countSupplier;
        }

        public String getRepositoryUrl() {
            return queueName;
        }

        public long getRemainingCount() {
            return countSupplier.get();
        }

        public long getInitialCount() {
            return initialCount;
        }
    }

    private final ManageableMailQueue mailQueue;
    private final AdditionalInformation additionalInformation;

    public ClearMailQueueTask(ManageableMailQueue mailQueue, String queueName) {
        this.mailQueue = mailQueue;
        this.additionalInformation = new AdditionalInformation(queueName, this::getRemainingSize);
    }

    @Override
    public Result run() {
        try {
            mailQueue.clear();
            return Result.COMPLETED;
        } catch (MessagingException e) {
            LOGGER.error("Encountered error while clearing queue", e);
            return Result.PARTIAL;
        }
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(additionalInformation);
    }

    public long getRemainingSize() {
        try {
            return mailQueue.getSize();
        } catch (MessagingException e) {
            throw Throwables.propagate(e);
        }
    }
}

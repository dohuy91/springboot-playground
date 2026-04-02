package com.springbootplayground.messaging.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/kafka")
public class QueueMessageController {

    private static final Logger log = LoggerFactory.getLogger(QueueMessageController.class);

    private final JobOperator jobOperator;
    private final Job kafkaRetryJob;

    public QueueMessageController(JobOperator jobOperator, Job kafkaRetryJob) {
        this.jobOperator = jobOperator;
        this.kafkaRetryJob = kafkaRetryJob;
    }

    @PostMapping("/retry")
    public ResponseEntity<String> triggerRetry() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("triggeredAt", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execution = jobOperator.start(kafkaRetryJob, params);
            log.info("kafkaRetryJob started: executionId={}, status={}", execution.getId(), execution.getStatus());
            return ResponseEntity.ok(
                    "kafkaRetryJob started - executionId=%d, status=%s"
                            .formatted(execution.getId(), execution.getStatus()));
        } catch (Exception ex) {
            log.error("Failed to launch kafkaRetryJob", ex);
            return ResponseEntity.internalServerError()
                    .body("Failed to launch kafkaRetryJob: " + ex.getMessage());
        }
    }
}

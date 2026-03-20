package com.springbootplayground.product.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaRetryJobController {

    private static final Logger log = LoggerFactory.getLogger(KafkaRetryJobController.class);

    private final JobLauncher jobLauncher;
    private final Job kafkaRetryJob;

    public KafkaRetryJobController(JobLauncher jobLauncher, Job kafkaRetryJob) {
        this.jobLauncher = jobLauncher;
        this.kafkaRetryJob = kafkaRetryJob;
    }

    /**
     * Manually triggers the Kafka retry batch job.
     * Each invocation uses a unique timestamp parameter so Spring Batch treats it as a new job instance.
     *
     * @return job execution ID and final batch status.
     */
    @PostMapping("/retry")
    public ResponseEntity<String> triggerRetry() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("triggeredAt", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(kafkaRetryJob, params);
            log.info("kafkaRetryJob started: executionId={}, status={}", execution.getId(), execution.getStatus());
            return ResponseEntity.ok(
                    "kafkaRetryJob started — executionId=%d, status=%s"
                            .formatted(execution.getId(), execution.getStatus()));
        } catch (Exception ex) {
            log.error("Failed to launch kafkaRetryJob", ex);
            return ResponseEntity.internalServerError()
                    .body("Failed to launch kafkaRetryJob: " + ex.getMessage());
        }
    }
}

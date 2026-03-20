package com.springbootplayground.product.batch;

import com.springbootplayground.common.config.KafkaProperties;
import com.springbootplayground.product.entity.KafkaMessageRecord;
import com.springbootplayground.product.repository.KafkaMessageRecordRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class KafkaRetryJobConfig {

    /**
     * Manual-trigger Spring Batch job that retries FAILED Kafka message records.
     * Trigger via POST /api/admin/kafka/retry – the job is never launched on startup.
     */
    @Bean
    public Job kafkaRetryJob(JobRepository jobRepository, Step kafkaRetryStep) {
        return new JobBuilder("kafkaRetryJob", jobRepository)
                .start(kafkaRetryStep)
                .build();
    }

    @Bean
    public Step kafkaRetryStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               KafkaMessageRecordRepository repository,
                               KafkaProperties properties,
                               KafkaRetryItemProcessor processor) {
        return new StepBuilder("kafkaRetryStep", jobRepository)
                .<KafkaMessageRecord, KafkaMessageRecord>chunk(10, transactionManager)
                .reader(retryItemReader(repository, properties))
                .processor(processor)
                .writer(retryItemWriter(repository))
                .build();
    }

    /**
     * Loads all retry-eligible records at step startup. Using a snapshot list is appropriate
     * here because this is a manual job operating on a bounded, typically small set of failures.
     */
    private ListItemReader<KafkaMessageRecord> retryItemReader(KafkaMessageRecordRepository repository,
                                                               KafkaProperties properties) {
        List<KafkaMessageRecord> eligible =
                repository.findRetryEligible(properties.getRetry().getMaxRetries(), Instant.now());
        return new ListItemReader<>(eligible);
    }

    private ItemWriter<KafkaMessageRecord> retryItemWriter(KafkaMessageRecordRepository repository) {
        return chunk -> repository.saveAll(chunk.getItems());
    }
}

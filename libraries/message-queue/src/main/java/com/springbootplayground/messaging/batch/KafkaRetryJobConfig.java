package com.springbootplayground.messaging.batch;

import com.springbootplayground.messaging.config.KafkaProperties;
import com.springbootplayground.messaging.entity.QueueMessage;
import com.springbootplayground.messaging.repository.KafkaMessageRecordRepository;
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
                .<QueueMessage, QueueMessage>chunk(10, transactionManager)
                .reader(retryItemReader(repository, properties))
                .processor(processor)
                .writer(retryItemWriter(repository))
                .build();
    }

    private ListItemReader<QueueMessage> retryItemReader(KafkaMessageRecordRepository repository,
                                                          KafkaProperties properties) {
        List<QueueMessage> eligible =
                repository.findRetryEligible(properties.getRetry().getMaxRetries(), Instant.now());
        return new ListItemReader<>(eligible);
    }

    private ItemWriter<QueueMessage> retryItemWriter(KafkaMessageRecordRepository repository) {
        return chunk -> repository.saveAll(chunk.getItems());
    }
}

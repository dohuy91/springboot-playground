package com.springbootplayground.messaging.batch;

import com.springbootplayground.messaging.config.KafkaProperties;
import com.springbootplayground.messaging.entity.MessageStatus;
import com.springbootplayground.messaging.entity.QueueMessage;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRetryItemProcessor implements ItemProcessor<QueueMessage, QueueMessage> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;

    @Override
    public QueueMessage process(QueueMessage record) {
        int maxRetries = properties.getRetry().getMaxRetries();

        if (record.getRetryCount() >= maxRetries) {
            log.warn("Exhausting record id={} (retryCount={} >= maxRetries={})",
                    record.getId(), record.getRetryCount(), maxRetries);
            record.setStatus(MessageStatus.EXHAUSTED);
            return record;
        }

        try {
            kafkaTemplate.send(record.getTopic(), record.getMessageKey(), record.getPayload()).get();
            log.info("Successfully replayed record id={} (source={}, topic={})",
                    record.getId(), record.getSourceType(), record.getTopic());
            record.setStatus(MessageStatus.SENT);
            record.setExceptionClass(null);
            record.setExceptionMessage(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleRetryFailure(record, e, maxRetries);
        } catch (ExecutionException e) {
            handleRetryFailure(record, (Exception) e.getCause(), maxRetries);
        }

        return record;
    }

    private void handleRetryFailure(QueueMessage record, Exception cause, int maxRetries) {
        int nextCount = record.getRetryCount() + 1;
        log.warn("Retry attempt {}/{} failed for record id={}: {}",
                nextCount, maxRetries, record.getId(), cause.getMessage());

        record.setRetryCount(nextCount);
        record.setExceptionClass(cause.getClass().getName());
        record.setExceptionMessage(cause.getMessage());

        if (nextCount >= maxRetries) {
            record.setStatus(MessageStatus.EXHAUSTED);
        } else {
            record.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        }
    }
}

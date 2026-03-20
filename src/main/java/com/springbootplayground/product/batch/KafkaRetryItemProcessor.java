package com.springbootplayground.product.batch;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.springbootplayground.common.config.KafkaProperties;
import com.springbootplayground.product.entity.KafkaMessageRecord;
import com.springbootplayground.product.entity.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attempts to replay a failed Kafka message record.
 * <ul>
 *   <li>PRODUCER failures: re-publishes the stored payload to the original topic.</li>
 *   <li>CONSUMER failures: re-publishes the payload so the listener can re-process it.</li>
 * </ul>
 * On success the record status is set to SENT; on failure the retry count is incremented
 * and the next retry window is scheduled. Records that exceed maxRetries are marked EXHAUSTED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRetryItemProcessor implements ItemProcessor<KafkaMessageRecord, KafkaMessageRecord> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;

    @Override
    public KafkaMessageRecord process(KafkaMessageRecord record) {
        int maxRetries = properties.getRetry().getMaxRetries();

        if (record.getRetryCount() >= maxRetries) {
            log.warn("Exhausting record id={} (retryCount={} >= maxRetries={})",
                    record.getId(), record.getRetryCount(), maxRetries);
            record.setStatus(MessageStatus.EXHAUSTED);
            return record;
        }

        try {
            // Both PRODUCER and CONSUMER failures are replayed by re-publishing to the original topic.
            // CONSUMER failures will be re-consumed by ProductEventListener.
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

    private void handleRetryFailure(KafkaMessageRecord record, Exception cause, int maxRetries) {
        int nextCount = record.getRetryCount() + 1;
        log.warn("Retry attempt {}/{} failed for record id={}: {}",
                nextCount, maxRetries, record.getId(), cause.getMessage());

        record.setRetryCount(nextCount);
        record.setExceptionClass(cause != null ? cause.getClass().getName() : null);
        record.setExceptionMessage(cause != null ? cause.getMessage() : null);

        if (nextCount >= maxRetries) {
            record.setStatus(MessageStatus.EXHAUSTED);
        } else {
            record.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        }
    }
}

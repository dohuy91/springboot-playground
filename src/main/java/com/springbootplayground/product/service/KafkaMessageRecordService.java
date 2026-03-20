package com.springbootplayground.product.service;

import com.springbootplayground.common.config.KafkaProperties;
import com.springbootplayground.product.entity.KafkaMessageRecord;
import com.springbootplayground.product.entity.MessageStatus;
import com.springbootplayground.product.entity.SourceType;
import com.springbootplayground.product.repository.KafkaMessageRecordRepository;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaMessageRecordService {

    private final KafkaMessageRecordRepository repository;
    private final KafkaProperties properties;

    /** Records a successful producer send. Only persisted when storage mode is ALL. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerSent(String topic, String key, String payload) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            repository.save(KafkaMessageRecord.sent(SourceType.PRODUCER, topic, key, payload));
        }
    }

    /** Always persisted regardless of storage mode. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerFailed(String topic, String key, String payload, Exception ex) {
        repository.save(KafkaMessageRecord.failed(SourceType.PRODUCER, topic, key, payload, ex));
    }

    /** Records a successfully processed consumer record. Only persisted when storage mode is ALL. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerSuccess(ConsumerRecord<String, String> record) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            KafkaMessageRecord r = KafkaMessageRecord.sent(
                    SourceType.CONSUMER, record.topic(), record.key(), record.value());
            r.setPartitionNumber(record.partition());
            r.setMessageOffset(record.offset());
            repository.save(r);
        }
    }

    /** Always persisted when the consumer listener exhausts its retries. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerFailed(ConsumerRecord<?, ?> record, Exception ex) {
        KafkaMessageRecord r = KafkaMessageRecord.failed(
                SourceType.CONSUMER,
                record.topic(),
                record.key() != null ? record.key().toString() : null,
                record.value() != null ? record.value().toString() : null,
                ex);
        r.setPartitionNumber(record.partition());
        r.setMessageOffset(record.offset());
        repository.save(r);
    }

    /** Marks a record as successfully replayed by the batch job. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(KafkaMessageRecord record) {
        record.setStatus(MessageStatus.SENT);
        record.setExceptionClass(null);
        record.setExceptionMessage(null);
        repository.save(record);
    }

    /** Increments retry count and schedules the next batch retry attempt. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetrying(KafkaMessageRecord record) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        repository.save(record);
    }

    /** Marks a record as permanently exhausted after max retries are exceeded. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExhausted(KafkaMessageRecord record) {
        record.setStatus(MessageStatus.EXHAUSTED);
        repository.save(record);
    }
}

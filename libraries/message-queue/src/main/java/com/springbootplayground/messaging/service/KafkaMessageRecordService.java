package com.springbootplayground.messaging.service;

import com.springbootplayground.messaging.config.KafkaProperties;
import com.springbootplayground.messaging.entity.MessageStatus;
import com.springbootplayground.messaging.entity.QueueMessage;
import com.springbootplayground.messaging.entity.SourceType;
import com.springbootplayground.messaging.repository.KafkaMessageRecordRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KafkaMessageRecordService {

    private final KafkaMessageRecordRepository repository;
    private final KafkaProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerSent(String topic, String key, String payload) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            repository.save(QueueMessage.sent(SourceType.PRODUCER, topic, key, payload));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerFailed(String topic, String key, String payload, Exception ex) {
        repository.save(QueueMessage.failed(SourceType.PRODUCER, topic, key, payload, ex));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerSuccess(ConsumerRecord<String, String> record) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            QueueMessage r = QueueMessage.sent(SourceType.CONSUMER, record.topic(), record.key(), record.value());
            r.setPartitionNumber(record.partition());
            r.setMessageOffset(record.offset());
            repository.save(r);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerFailed(ConsumerRecord<?, ?> record, Exception ex) {
        QueueMessage r = QueueMessage.failed(
                SourceType.CONSUMER,
                record.topic(),
                record.key() != null ? record.key().toString() : null,
                record.value() != null ? record.value().toString() : null,
                ex);
        r.setPartitionNumber(record.partition());
        r.setMessageOffset(record.offset());
        repository.save(r);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(QueueMessage record) {
        record.setStatus(MessageStatus.SENT);
        record.setExceptionClass(null);
        record.setExceptionMessage(null);
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetrying(QueueMessage record) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExhausted(QueueMessage record) {
        record.setStatus(MessageStatus.EXHAUSTED);
        repository.save(record);
    }
}

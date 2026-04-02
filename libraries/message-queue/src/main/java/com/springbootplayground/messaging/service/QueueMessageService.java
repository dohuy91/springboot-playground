package com.springbootplayground.messaging.service;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.springbootplayground.messaging.config.KafkaProperties;
import com.springbootplayground.messaging.entity.MessageStatus;
import com.springbootplayground.messaging.entity.QueueMessage;
import com.springbootplayground.messaging.entity.SourceType;
import com.springbootplayground.messaging.repository.QueueMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueMessageService {

    private final QueueMessageRepository queueMessageRepository;
    private final KafkaProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerSent(String topic, String key, String payload) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            queueMessageRepository.save(QueueMessage.sent(SourceType.PRODUCER, topic, key, payload));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProducerFailed(String topic, String key, String payload, Exception ex) {
        queueMessageRepository.save(QueueMessage.failed(SourceType.PRODUCER, topic, key, payload, ex));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerSuccess(ConsumerRecord<String, String> record) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            QueueMessage r = QueueMessage.sent(SourceType.CONSUMER, record.topic(), record.key(), record.value());
            r.setPartitionNumber(record.partition());
            r.setMessageOffset(record.offset());
            queueMessageRepository.save(r);
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
        queueMessageRepository.save(r);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(QueueMessage record) {
        record.setStatus(MessageStatus.SENT);
        record.setExceptionClass(null);
        record.setExceptionMessage(null);
        queueMessageRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetrying(QueueMessage record) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        queueMessageRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExhausted(QueueMessage record) {
        record.setStatus(MessageStatus.EXHAUSTED);
        queueMessageRepository.save(record);
    }
}

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
    public void recordConsumerSuccess(ConsumerRecord<String, String> consumerRecord) {
        if (properties.getStorageMode() == KafkaProperties.StorageMode.ALL) {
            QueueMessage r = QueueMessage.sent(SourceType.CONSUMER, consumerRecord.topic(), consumerRecord.key(), consumerRecord.value());
            r.setPartitionNumber(consumerRecord.partition());
            r.setMessageOffset(consumerRecord.offset());
            queueMessageRepository.save(r);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerFailed(ConsumerRecord<?, ?> consumerRecord, Exception ex) {
        QueueMessage r = QueueMessage.failed(
                SourceType.CONSUMER,
                consumerRecord.topic(),
                consumerRecord.key() != null ? consumerRecord.key().toString() : null,
                consumerRecord.value() != null ? consumerRecord.value().toString() : null,
                ex);
        r.setPartitionNumber(consumerRecord.partition());
        r.setMessageOffset(consumerRecord.offset());
        queueMessageRepository.save(r);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(QueueMessage queueMessage) {
        queueMessage.setStatus(MessageStatus.SENT);
        queueMessage.setExceptionClass(null);
        queueMessage.setExceptionMessage(null);
        queueMessageRepository.save(queueMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetrying(QueueMessage queueMessage) {
        queueMessage.setRetryCount(queueMessage.getRetryCount() + 1);
        queueMessage.setNextRetryAt(Instant.now().plusSeconds(properties.getRetry().getBackoffSeconds()));
        queueMessageRepository.save(queueMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExhausted(QueueMessage queueMessage) {
        queueMessage.setStatus(MessageStatus.EXHAUSTED);
        queueMessageRepository.save(queueMessage);
    }
}

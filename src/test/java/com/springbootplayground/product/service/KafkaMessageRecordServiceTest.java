package com.springbootplayground.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springbootplayground.common.config.KafkaProperties;
import com.springbootplayground.product.entity.KafkaMessageRecord;
import com.springbootplayground.product.entity.MessageStatus;
import com.springbootplayground.product.entity.SourceType;
import com.springbootplayground.product.repository.KafkaMessageRecordRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaMessageRecordServiceTest {

    @Mock
    private KafkaMessageRecordRepository repository;

    @Mock
    private KafkaProperties properties;

    private KafkaMessageRecordService service;

    @BeforeEach
    void setUp() {
        service = new KafkaMessageRecordService(repository, properties);
    }

    // -----------------------------------------------------------------------
    // Producer – sent
    // -----------------------------------------------------------------------

    @Test
    void recordProducerSentDoesNotPersistWhenModeIsFailedOnly() {
        when(properties.getStorageMode()).thenReturn(KafkaProperties.StorageMode.FAILED_ONLY);

        service.recordProducerSent("products", "sku-1", "{\"sku\":\"sku-1\"}");

        verify(repository, never()).save(any());
    }

    @Test
    void recordProducerSentPersistsWithStatusSentWhenModeIsAll() {
        when(properties.getStorageMode()).thenReturn(KafkaProperties.StorageMode.ALL);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordProducerSent("products", "sku-1", "{\"sku\":\"sku-1\"}");

        ArgumentCaptor<KafkaMessageRecord> captor = ArgumentCaptor.forClass(KafkaMessageRecord.class);
        verify(repository).save(captor.capture());
        KafkaMessageRecord saved = captor.getValue();
        assertEquals(MessageStatus.SENT, saved.getStatus());
        assertEquals(SourceType.PRODUCER, saved.getSourceType());
        assertEquals("products", saved.getTopic());
        assertEquals("sku-1", saved.getMessageKey());
    }

    // -----------------------------------------------------------------------
    // Producer – failed
    // -----------------------------------------------------------------------

    @Test
    void recordProducerFailedAlwaysPersistsRegardlessOfMode() {
        // No stubbing on storageMode – service must NOT check it for failures.
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordProducerFailed("products", "sku-2", "{}", new RuntimeException("broker down"));

        ArgumentCaptor<KafkaMessageRecord> captor = ArgumentCaptor.forClass(KafkaMessageRecord.class);
        verify(repository).save(captor.capture());
        KafkaMessageRecord saved = captor.getValue();
        assertEquals(MessageStatus.FAILED, saved.getStatus());
        assertEquals(SourceType.PRODUCER, saved.getSourceType());
        assertEquals("broker down", saved.getExceptionMessage());
    }

    // -----------------------------------------------------------------------
    // Consumer – success
    // -----------------------------------------------------------------------

    @Test
    void recordConsumerSuccessDoesNotPersistWhenModeIsFailedOnly() {
        when(properties.getStorageMode()).thenReturn(KafkaProperties.StorageMode.FAILED_ONLY);

        service.recordConsumerSuccess(consumerRecord("products", "sku-3", "{}", 0, 10L));

        verify(repository, never()).save(any());
    }

    @Test
    void recordConsumerSuccessPersistsWithPartitionAndOffsetWhenModeIsAll() {
        when(properties.getStorageMode()).thenReturn(KafkaProperties.StorageMode.ALL);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordConsumerSuccess(consumerRecord("products", "sku-3", "{}", 2, 42L));

        ArgumentCaptor<KafkaMessageRecord> captor = ArgumentCaptor.forClass(KafkaMessageRecord.class);
        verify(repository).save(captor.capture());
        KafkaMessageRecord saved = captor.getValue();
        assertEquals(MessageStatus.SENT, saved.getStatus());
        assertEquals(SourceType.CONSUMER, saved.getSourceType());
        assertEquals(2, saved.getPartitionNumber());
        assertEquals(42L, saved.getMessageOffset());
    }

    // -----------------------------------------------------------------------
    // Consumer – failed
    // -----------------------------------------------------------------------

    @Test
    void recordConsumerFailedAlwaysPersists() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordConsumerFailed(
                consumerRecord("products", "sku-4", "bad-payload", 1, 5L),
                new RuntimeException("deserialization error"));

        ArgumentCaptor<KafkaMessageRecord> captor = ArgumentCaptor.forClass(KafkaMessageRecord.class);
        verify(repository).save(captor.capture());
        KafkaMessageRecord saved = captor.getValue();
        assertEquals(MessageStatus.FAILED, saved.getStatus());
        assertEquals(SourceType.CONSUMER, saved.getSourceType());
        assertEquals(1, saved.getPartitionNumber());
        assertEquals(5L, saved.getMessageOffset());
    }

    // -----------------------------------------------------------------------
    // Status transitions
    // -----------------------------------------------------------------------

    @Test
    void markSuccessSetsStatusToSentAndClearsException() {
        KafkaMessageRecord record = KafkaMessageRecord.failed(
                SourceType.PRODUCER, "products", "k", "{}", new RuntimeException("err"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markSuccess(record);

        assertEquals(MessageStatus.SENT, record.getStatus());
        assertEquals(null, record.getExceptionClass());
        assertEquals(null, record.getExceptionMessage());
    }

    @Test
    void markExhaustedSetsStatusToExhausted() {
        KafkaMessageRecord record = KafkaMessageRecord.failed(
                SourceType.CONSUMER, "products", "k", "{}", null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markExhausted(record);

        assertEquals(MessageStatus.EXHAUSTED, record.getStatus());
    }

    @Test
    void markRetryingIncrementsCountAndSchedulesNextRetry() {
        KafkaProperties.RetryProperties retryProps = new KafkaProperties.RetryProperties();
        retryProps.setBackoffSeconds(60);
        when(properties.getRetry()).thenReturn(retryProps);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KafkaMessageRecord record = KafkaMessageRecord.failed(
                SourceType.PRODUCER, "products", "k", "{}", null);

        service.markRetrying(record);

        assertEquals(1, record.getRetryCount());
        assertEquals(MessageStatus.FAILED, record.getStatus());
        // nextRetryAt should be approximately now + 60s
        long secondsUntilRetry = java.time.Duration.between(
                java.time.Instant.now(), record.getNextRetryAt()).getSeconds();
        org.junit.jupiter.api.Assertions.assertTrue(secondsUntilRetry >= 59 && secondsUntilRetry <= 61,
                "Expected ~60s backoff, got " + secondsUntilRetry + "s");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ConsumerRecord<String, String> consumerRecord(String topic, String key, String value,
                                                           int partition, long offset) {
        return new ConsumerRecord<>(topic, partition, offset, key, value);
    }
}

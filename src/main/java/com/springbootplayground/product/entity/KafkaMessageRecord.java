package com.springbootplayground.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "kafka_message_records")
@Getter
@Setter
public class KafkaMessageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MessageStatus status;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(name = "partition_number")
    private Integer partitionNumber;

    @Column(name = "message_offset")
    private Long messageOffset;

    @Column(name = "message_key", length = 256)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "exception_class", length = 256)
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static KafkaMessageRecord sent(SourceType sourceType, String topic, String messageKey, String payload) {
        KafkaMessageRecord r = new KafkaMessageRecord();
        r.sourceType = sourceType;
        r.status = MessageStatus.SENT;
        r.topic = topic;
        r.messageKey = messageKey;
        r.payload = payload;
        r.retryCount = 0;
        return r;
    }

    public static KafkaMessageRecord failed(SourceType sourceType, String topic, String messageKey,
                                            String payload, Exception ex) {
        KafkaMessageRecord r = new KafkaMessageRecord();
        r.sourceType = sourceType;
        r.status = MessageStatus.FAILED;
        r.topic = topic;
        r.messageKey = messageKey;
        r.payload = payload != null ? payload : "";
        if (ex != null) {
            r.exceptionClass = ex.getClass().getName();
            r.exceptionMessage = ex.getMessage();
        }
        r.retryCount = 0;
        return r;
    }
}

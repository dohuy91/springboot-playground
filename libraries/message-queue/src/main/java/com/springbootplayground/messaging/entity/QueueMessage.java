package com.springbootplayground.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "queue_messages")
@Getter
@Setter
public class QueueMessage extends Entity {

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

    public static QueueMessage sent(SourceType sourceType, String topic, String messageKey, String payload) {
        QueueMessage r = new QueueMessage();
        r.sourceType = sourceType;
        r.status = MessageStatus.SENT;
        r.topic = topic;
        r.messageKey = messageKey;
        r.payload = payload;
        r.retryCount = 0;
        return r;
    }

    public static QueueMessage failed(SourceType sourceType, String topic, String messageKey,
                                       String payload, Exception ex) {
        QueueMessage r = new QueueMessage();
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

package com.springbootplayground.messaging.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.springbootplayground.messaging.entity.MessageStatus;
import com.springbootplayground.messaging.entity.QueueMessage;

public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {

    @Query("""
            SELECT r FROM QueueMessage r
            WHERE r.status = 'FAILED'
              AND r.retryCount < :maxRetries
              AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= :now)
            ORDER BY r.createdAt ASC
            """)
    List<QueueMessage> findRetryEligible(@Param("maxRetries") int maxRetries,
                                         @Param("now") Instant now);

    Page<QueueMessage> findByStatus(MessageStatus status, Pageable pageable);
}

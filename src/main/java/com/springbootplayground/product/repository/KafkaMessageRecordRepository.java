package com.springbootplayground.product.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.springbootplayground.product.entity.KafkaMessageRecord;
import com.springbootplayground.product.entity.MessageStatus;

public interface KafkaMessageRecordRepository extends JpaRepository<KafkaMessageRecord, Long> {

    /**
     * Returns FAILED records whose retry_count is below the configured limit and whose
     * scheduled retry time has arrived (or has not been set yet).
     */
    @Query("""
            SELECT r FROM KafkaMessageRecord r
            WHERE r.status = 'FAILED'
              AND r.retryCount < :maxRetries
              AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= :now)
            ORDER BY r.createdAt ASC
            """)
    List<KafkaMessageRecord> findRetryEligible(@Param("maxRetries") int maxRetries,
                                               @Param("now") Instant now);

    Page<KafkaMessageRecord> findByStatus(MessageStatus status, Pageable pageable);
}

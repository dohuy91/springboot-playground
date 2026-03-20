CREATE TABLE IF NOT EXISTS kafka_message_records (
    id              BIGSERIAL PRIMARY KEY,
    source_type     VARCHAR(16)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    partition_number INTEGER,
    message_offset  BIGINT,
    message_key     VARCHAR(256),
    payload         TEXT         NOT NULL,
    exception_class VARCHAR(256),
    exception_message TEXT,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kmr_status        ON kafka_message_records (status);
CREATE INDEX IF NOT EXISTS idx_kmr_retry_eligible ON kafka_message_records (status, retry_count, next_retry_at)
    WHERE status = 'FAILED';

package com.springbootplayground.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.kafka")
@Getter
@Setter
public class KafkaProperties {

    private StorageMode storageMode = StorageMode.FAILED_ONLY;
    private RetryProperties retry = new RetryProperties();

    public enum StorageMode {
        ALL, FAILED_ONLY
    }

    @Getter
    @Setter
    public static class RetryProperties {
        /** Number of additional retries via the batch job (not the consumer's own retries). */
        private int maxRetries = 3;
        /** Seconds to wait before the next batch retry attempt. */
        private long backoffSeconds = 30;
    }
}

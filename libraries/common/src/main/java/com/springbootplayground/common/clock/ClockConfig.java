package com.springbootplayground.common.clock;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a UTC {@link Clock} bean so that {@link SystemClockProvider}
 * and any other component can inject a testable clock abstraction.
 * <p>
 * Override this bean in tests using {@code @TestConfiguration} + {@code @Primary}
 * to freeze time at a deterministic instant.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}


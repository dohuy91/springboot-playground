package com.springbootplayground.common.clock;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Production {@link ClockProvider} that delegates to a {@link Clock} bean.
 * <p>
 * Swap the {@link Clock} bean for {@link Clock#fixed} in tests to control time.
 */
@Component
public class SystemClockProvider implements ClockProvider {

    private final Clock clock;

    public SystemClockProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}


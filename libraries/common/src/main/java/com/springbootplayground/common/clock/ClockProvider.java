package com.springbootplayground.common.clock;

import java.time.Instant;

/**
 * Abstraction over the system clock.
 * <p>
 * Inject this interface instead of calling {@link Instant#now()} directly
 * so that the clock can be replaced with a fixed instant in tests.
 */
public interface ClockProvider {

    /**
     * Returns the current instant according to this clock.
     */
    Instant now();
}


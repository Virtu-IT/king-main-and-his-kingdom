package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.exception.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exercise B3: counts failed logins per username and blocks the username for a while after too many.
 * In memory, so it is per application instance: with several instances (a cluster) you need a shared
 * store (a database table or Redis), otherwise an attacker gets the limit once per instance.
 * Unknown usernames are counted and blocked exactly like known ones, so the answer never reveals which
 * accounts exist.
 */
@Service
public class LoginAttemptService {

    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock,
                               @Value("${kingdom.security.max-login-attempts:3}") int maxAttempts,
                               @Value("${kingdom.security.login-lock-duration:1m}") Duration lockDuration) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    public boolean isLocked(String username) {
        Attempts current = attempts.get(key(username));
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant());
    }

    /** Throws when the username is blocked; the exception knows how long it takes. */
    public void assertNotLocked(String username) {
        Attempts current = attempts.get(key(username));
        if (current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant())) {
            long seconds = Math.max(1, Duration.between(clock.instant(), current.lockedUntil()).plusMillis(999).toSeconds());
            throw new TooManyAttemptsException(seconds);
        }
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        attempts.compute(key(username), (k, current) -> {
            // a lock that has expired starts a new series
            int failures = (current == null || (current.lockedUntil() != null && !current.lockedUntil().isAfter(now)))
                    ? 1 : current.failures() + 1;
            return new Attempts(failures, failures >= maxAttempts ? now.plus(lockDuration) : null);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant lockedUntil) {
    }
}

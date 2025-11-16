package it.dinevo.auth.services;

import it.dinevo.auth.exception.RateLimitException;
import it.dinevo.auth.services.interfaces.RateLimitStore;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitStore implements RateLimitStore {

    private final ConcurrentHashMap<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptTracker> otpAttempts = new ConcurrentHashMap<>();

    @Override
    public AttemptTracker recordAttempt(String identifier, int maxAttempts, int windowMinutes, String action) {
        ConcurrentHashMap<String, AttemptTracker> attempts = getAttemptsMap(action);

        return attempts.compute(identifier, (key, existing) -> {
            if (existing == null) {
                return new AttemptTracker(1, LocalDateTime.now());
            }

            // Reset if window has passed
            if (existing.firstAttempt().plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
                return new AttemptTracker(1, LocalDateTime.now());
            }

            // Check if limit exceeded
            if (existing.count() >= maxAttempts) {
                throw new RateLimitException("Too many " + action + " attempts. Please try again later.");
            }

            // Increment count
            return new AttemptTracker(existing.count() + 1, existing.firstAttempt());
        });
    }

    @Override
    public void reset(String identifier) {
        loginAttempts.remove(identifier);
        otpAttempts.remove(identifier);
    }

    @Override
    public void cleanup(int loginWindowMinutes, int otpWindowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        loginAttempts.entrySet().removeIf(entry ->
            entry.getValue().firstAttempt().plusMinutes(loginWindowMinutes).isBefore(now)
        );
        otpAttempts.entrySet().removeIf(entry ->
            entry.getValue().firstAttempt().plusMinutes(otpWindowMinutes).isBefore(now)
        );
    }

    private ConcurrentHashMap<String, AttemptTracker> getAttemptsMap(String action) {
        return action.equalsIgnoreCase("login") ? loginAttempts : otpAttempts;
    }
}

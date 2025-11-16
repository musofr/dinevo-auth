package it.dinevo.auth.services.interfaces;

import java.time.LocalDateTime;

public interface RateLimitStore {

    /**
     * Records an attempt for the given identifier and returns the updated tracker.
     * Implements rate limit logic: resets if window expired, throws exception if limit exceeded.
     *
     * @param identifier unique identifier (e.g., email, IP)
     * @param maxAttempts maximum allowed attempts
     * @param windowMinutes time window in minutes
     * @param action action description for error messages
     * @return updated attempt tracker
     * @throws it.dinevo.auth.exception.RateLimitException if rate limit exceeded
     */
    AttemptTracker recordAttempt(String identifier, int maxAttempts, int windowMinutes, String action);

    /**
     * Resets attempts for the given identifier.
     *
     * @param identifier unique identifier to reset
     */
    void reset(String identifier);

    /**
     * Cleans up expired entries.
     *
     * @param loginWindowMinutes login window in minutes
     * @param otpWindowMinutes OTP window in minutes
     */
    void cleanup(int loginWindowMinutes, int otpWindowMinutes);

    record AttemptTracker(int count, LocalDateTime firstAttempt) {}
}

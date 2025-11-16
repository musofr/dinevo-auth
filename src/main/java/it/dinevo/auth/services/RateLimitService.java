package it.dinevo.auth.services;

import io.quarkus.scheduler.Scheduled;
import it.dinevo.auth.services.interfaces.RateLimitStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RateLimitService {

    @ConfigProperty(name = "rate-limit.login.max-attempts", defaultValue = "5")
    int loginMaxAttempts;

    @ConfigProperty(name = "rate-limit.login.window-minutes", defaultValue = "15")
    int loginWindowMinutes;

    @ConfigProperty(name = "rate-limit.otp.max-attempts", defaultValue = "3")
    int otpMaxAttempts;

    @ConfigProperty(name = "rate-limit.otp.window-minutes", defaultValue = "5")
    int otpWindowMinutes;

    @Inject
    RateLimitStore store;

    public void checkLoginRateLimit(String identifier) {
        store.recordAttempt(identifier, loginMaxAttempts, loginWindowMinutes, "login");
    }

    public void checkOtpRateLimit(String identifier) {
        store.recordAttempt(identifier, otpMaxAttempts, otpWindowMinutes, "OTP");
    }

    public void resetLoginAttempts(String identifier) {
        store.reset(identifier);
    }

    // Cleanup expired entries every hour
    @Scheduled(every = "1h")
    void cleanupExpiredEntries() {
        store.cleanup(loginWindowMinutes, otpWindowMinutes);
    }
}

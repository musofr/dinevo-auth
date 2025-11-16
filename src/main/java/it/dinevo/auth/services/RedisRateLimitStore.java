package it.dinevo.auth.services;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import it.dinevo.auth.exception.RateLimitException;
import it.dinevo.auth.services.interfaces.RateLimitStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedisRateLimitStore implements RateLimitStore {

    private static final String LOGIN_PREFIX = "rate-limit:login:";
    private static final String OTP_PREFIX = "rate-limit:otp:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ValueCommands<String, String> commands;

    public RedisRateLimitStore(RedisDataSource dataSource) {
        this.commands = dataSource.value(String.class);
    }

    @Override
    public AttemptTracker recordAttempt(String identifier, int maxAttempts, int windowMinutes, String action) {
        String prefix = action.equalsIgnoreCase("login") ? LOGIN_PREFIX : OTP_PREFIX;
        String countKey = prefix + identifier + ":count";
        String timestampKey = prefix + identifier + ":timestamp";

        String timestampStr = commands.get(timestampKey);
        LocalDateTime now = LocalDateTime.now();

        // If no existing attempt or window expired
        if (timestampStr == null) {
            commands.set(countKey, "1", new io.quarkus.redis.datasource.value.SetArgs().ex(Duration.ofMinutes(windowMinutes)));
            commands.set(timestampKey, now.format(FORMATTER), new io.quarkus.redis.datasource.value.SetArgs().ex(Duration.ofMinutes(windowMinutes)));
            return new AttemptTracker(1, now);
        }

        LocalDateTime firstAttempt = LocalDateTime.parse(timestampStr, FORMATTER);

        // Reset if window has passed
        if (firstAttempt.plusMinutes(windowMinutes).isBefore(now)) {
            commands.set(countKey, "1", new io.quarkus.redis.datasource.value.SetArgs().ex(Duration.ofMinutes(windowMinutes)));
            commands.set(timestampKey, now.format(FORMATTER), new io.quarkus.redis.datasource.value.SetArgs().ex(Duration.ofMinutes(windowMinutes)));
            return new AttemptTracker(1, now);
        }

        // Get current count
        String countStr = commands.get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        // Check if limit exceeded
        if (count >= maxAttempts) {
            throw new RateLimitException("Too many " + action + " attempts. Please try again later.");
        }

        // Increment count
        int newCount = count + 1;
        commands.set(countKey, String.valueOf(newCount), new io.quarkus.redis.datasource.value.SetArgs().ex(Duration.ofMinutes(windowMinutes)));

        return new AttemptTracker(newCount, firstAttempt);
    }

    @Override
    public void reset(String identifier) {
        commands.getdel(LOGIN_PREFIX + identifier + ":count");
        commands.getdel(LOGIN_PREFIX + identifier + ":timestamp");
        commands.getdel(OTP_PREFIX + identifier + ":count");
        commands.getdel(OTP_PREFIX + identifier + ":timestamp");
    }

    @Override
    public void cleanup(int loginWindowMinutes, int otpWindowMinutes) {
        // Redis automatically handles TTL expiration, no manual cleanup needed
    }
}

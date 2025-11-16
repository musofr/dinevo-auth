package it.dinevo.auth.services;

import io.quarkus.redis.datasource.RedisDataSource;
import it.dinevo.auth.services.interfaces.RateLimitStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RateLimitStoreProducer {

    @ConfigProperty(name = "rate-limit.store.type", defaultValue = "memory")
    String storeType;

    @Inject
    Instance<RedisDataSource> redisDataSource;

    @Produces
    @ApplicationScoped
    public RateLimitStore produceRateLimitStore() {
        return switch (storeType.toLowerCase()) {
            case "redis" -> {
                if (redisDataSource.isUnsatisfied()) {
                    throw new IllegalStateException("Redis store requested but RedisDataSource is not available");
                }
                yield new RedisRateLimitStore(redisDataSource.get());
            }
            default -> new InMemoryRateLimitStore();
        };
    }
}

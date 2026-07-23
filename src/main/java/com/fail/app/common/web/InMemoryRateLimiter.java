package com.fail.app.common.web;

import com.fail.app.common.config.RateLimitProperties;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;
    private final Object capacityLock = new Object();

    public InMemoryRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    public Decision tryAcquire(String key, RateLimitProperties.Limit policy) {
        long now = Instant.now().getEpochSecond();
        ensureCapacity(key, now);

        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtEpochSecond() <= now) {
                return new Window(1, now + policy.windowSeconds());
            }
            return new Window(current.count() + 1, current.expiresAtEpochSecond());
        });
        boolean allowed = updated.count() <= policy.limit();
        long retryAfter = Math.max(1, updated.expiresAtEpochSecond() - now);
        return new Decision(allowed, retryAfter);
    }

    int size() {
        return windows.size();
    }

    private void ensureCapacity(String key, long now) {
        if (windows.containsKey(key) || windows.size() < properties.maxEntries()) {
            return;
        }
        synchronized (capacityLock) {
            windows.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochSecond() <= now);
            while (!windows.containsKey(key) && windows.size() >= properties.maxEntries()) {
                windows.entrySet().stream()
                        .min(Comparator.comparingLong(entry -> entry.getValue().expiresAtEpochSecond()))
                        .map(java.util.Map.Entry::getKey)
                        .ifPresent(windows::remove);
            }
        }
    }

    private record Window(int count, long expiresAtEpochSecond) {
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }
}

package com.msig.claimsapi.service.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * In-memory LRU cache for AI extraction results.
 * Key = MD5 hash of raw input text; value = cached AIClient.ExtractedClaimData.
 * Same input within 5 minutes returns the cached result to save cost and latency.
 */
@Service
@Slf4j
public class AICacheService {

    private static final int MAX_SIZE = 1_000;
    private static final Duration EXPIRE_AFTER_WRITE = Duration.ofMinutes(5);

    private final Cache<String, AIClient.ExtractedClaimData> extractClaimDataCache;

    public AICacheService(MeterRegistry meterRegistry) {
        this.extractClaimDataCache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_WRITE)
            .recordStats()
            .build();
        log.info("[AICache] Initialised extractClaimData cache: maxSize={}, expireAfterWrite={}",
            MAX_SIZE, EXPIRE_AFTER_WRITE);

        Gauge.builder("ai.cache.hit_rate", extractClaimDataCache, c -> c.stats().hitRate())
            .description("AI extraction cache hit rate")
            .register(meterRegistry);

        Gauge.builder("ai.cache.eviction_count", extractClaimDataCache, c -> (double) c.stats().evictionCount())
            .description("AI extraction cache eviction count")
            .register(meterRegistry);

        Gauge.builder("ai.cache.size", extractClaimDataCache, c -> (double) c.estimatedSize())
            .description("AI extraction cache estimated size")
            .register(meterRegistry);
    }

    /**
     * Get cached extraction result, or null if not present.
     */
    public AIClient.ExtractedClaimData getIfPresent(String rawText) {
        String key = md5Key(rawText);
        AIClient.ExtractedClaimData cached = extractClaimDataCache.getIfPresent(key);
        if (cached != null) {
            log.debug("[AICache] Cache HIT for key={}", key);
        } else {
            log.debug("[AICache] Cache MISS for key={}", key);
        }
        return cached;
    }

    /**
     * Store an extraction result in the cache.
     */
    public void put(String rawText, AIClient.ExtractedClaimData data) {
        String key = md5Key(rawText);
        extractClaimDataCache.put(key, data);
        log.debug("[AICache] Cached result for key={}", key);
    }

    /**
     * Invalidate a single cache entry.
     */
    public void invalidate(String rawText) {
        extractClaimDataCache.invalidate(md5Key(rawText));
    }

    /**
     * Invalidate all entries.
     */
    public void invalidateAll() {
        extractClaimDataCache.invalidateAll();
        log.info("[AICache] All entries invalidated");
    }

    /**
     * Returns cache statistics string for logging/observability.
     */
    public String stats() {
        var stats = extractClaimDataCache.stats();
        return String.format("hitCount=%d missCount=%d hitRate=%.3f evictionCount=%d",
            stats.hitCount(), stats.missCount(), stats.hitRate(), stats.evictionCount());
    }

    private String md5Key(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: use hashCode (not great but won't crash)
            return String.valueOf(text.hashCode());
        }
    }
}

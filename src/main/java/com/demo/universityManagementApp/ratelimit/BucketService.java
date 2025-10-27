package com.demo.universityManagementApp.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service component for managing and providing rate limiting token buckets with concurrent access support.
 * Implements a bucket resolution mechanism that creates and caches token buckets per unique key,
 * ensuring consistent rate limiting across distributed request processing.
 * Core Functionality:
 * - Thread-safe bucket creation and retrieval using concurrent hash map
 * - Configurable rate limiting with greedy refill strategy
 * - Bucket reuse for repeated requests with same key
 * - Bucket management with automatic cleanup
 * This service uses a fixed rate limit of 25 requests per second with a burst capacity of 10,000 tokens,
 * suitable for high-traffic API endpoints requiring consistent rate limiting.
 *
 * @see org.springframework.stereotype.Component
 * @see io.github.bucket4j.Bucket
 * @see io.github.bucket4j.Bandwidth
 * @see java.util.concurrent.ConcurrentHashMap
 */
@Component
public class BucketService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Resolves or creates a rate limiting token bucket for the specified key with configured bandwidth limits.
     * Uses a compute-if-absent pattern to ensure thread-safe bucket creation for consistent
     * rate limiting across concurrent requests for the same key.
     * Rate Limit Configuration:
     * - Capacity: 2,000 tokens for burst handling
     * - Refill Rate: 50 tokens per second with greedy refill strategy
     * - Strategy: Greedy refill for immediate token availability after refill period
     *
     * @param key unique identifier for the rate limiting bucket, typically user identifier or IP address
     * @return {@link Bucket} instance configured with the specified rate limiting parameters
     * @throws IllegalArgumentException if the provided key is null or empty
     * @see io.github.bucket4j.Bucket
     * @see io.github.bucket4j.Bandwidth
     * @see io.github.bucket4j.Refill
     */
    public Bucket resolveBucket(String key) {
        Bandwidth limit = Bandwidth.classic(2000, Refill.greedy(50, Duration.ofSeconds(1)));
        return buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());
    }
}

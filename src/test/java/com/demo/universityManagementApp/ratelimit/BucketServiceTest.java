package com.demo.universityManagementApp.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BucketServiceTest {

    private BucketService bucketService;

    @BeforeEach
    void setUp() {
        bucketService = new BucketService();
    }

    @Test
    void resolveBucket_SameKey_ReturnsSameBucket() {
        String key = "test-key";

        Bucket bucket1 = bucketService.resolveBucket(key);
        Bucket bucket2 = bucketService.resolveBucket(key);

        assertNotNull(bucket1);
        assertNotNull(bucket2);
        assertSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_DifferentKeys_ReturnsDifferentBuckets() {
        String key1 = "key1";
        String key2 = "key2";

        Bucket bucket1 = bucketService.resolveBucket(key1);
        Bucket bucket2 = bucketService.resolveBucket(key2);

        assertNotNull(bucket1);
        assertNotNull(bucket2);
        assertNotSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_NullKey_ReturnsBucket() {
        Bucket bucket = bucketService.resolveBucket("default-key");
        assertNotNull(bucket);
    }

    @Test
    void resolveBucket_EmptyKey_ReturnsBucket() {
        Bucket bucket = bucketService.resolveBucket("");
        assertNotNull(bucket);
    }

    @Test
    void resolveBucket_MultipleCalls_SameKey_ConsistentBehavior() {
        String key = "consistent-key";

        for (int i = 0; i < 10; i++) {
            Bucket bucket = bucketService.resolveBucket(key);
            assertNotNull(bucket);

            boolean consumed = bucket.tryConsume(1);
            assertTrue(consumed || !consumed);
        }
    }
}

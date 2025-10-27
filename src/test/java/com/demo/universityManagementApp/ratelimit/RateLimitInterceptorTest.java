package com.demo.universityManagementApp.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private BucketService bucketService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        rateLimitInterceptor = new RateLimitInterceptor(bucketService, mapper);
    }

    @Test
    void preHandle_WithinRateLimit_ReturnsTrue() throws Exception {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mockConsumptionProbe(true, 0);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(bucketService.resolveBucket("ip: 127.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void preHandle_RateLimitExceeded_ReturnsFalse() throws Exception {
        String ip = "127.0.0.1";
        Bucket bucket = mock(Bucket.class);

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(5_000_000_000L);
        when(probe.getRemainingTokens()).thenReturn(0L);

        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        when(request.getRemoteAddr()).thenReturn(ip);
        when(bucketService.resolveBucket("ip: " + ip)).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
        //verify(printWriter).flush();
    }

    @Test
    void preHandle_WithXForwardedForHeader_UsesFirstIP() throws Exception {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mockConsumptionProbe(true, 0);

        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(bucketService.resolveBucket("ip: 192.168.1.1")).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(bucketService).resolveBucket("ip: 192.168.1.1");
    }

    @Test
    void preHandle_WithUserPrincipal_UsesUsername() throws Exception {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mockConsumptionProbe(true, 0);

        when(request.getUserPrincipal()).thenReturn(() -> "testuser");
        when(bucketService.resolveBucket("user: testuser")).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(bucketService).resolveBucket("user: testuser");
    }

    @Test
    void preHandle_UserPrincipalTakesPrecedenceOverIP() throws Exception {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mockConsumptionProbe(true, 0);

        when(request.getUserPrincipal()).thenReturn(() -> "testuser");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(bucketService.resolveBucket("user: testuser")).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(bucketService).resolveBucket("user: testuser");
        verify(bucketService, never()).resolveBucket("ip: 127.0.0.1");
    }

    private ConsumptionProbe mockConsumptionProbe(boolean consumed, long nanosToWait) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(consumed);
        when(probe.getNanosToWaitForRefill()).thenReturn(nanosToWait);
        when(probe.getRemainingTokens()).thenReturn(consumed ? 99L : 0L);
        return probe;
    }
}

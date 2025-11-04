package com.github.radupana.featherweight.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RateLimiterTest {
    private lateinit var context: Context
    private lateinit var rateLimiter: RateLimiter
    private val prefsDataByName = mutableMapOf<String, MutableMap<String, String>>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } answers {
            val prefsName = arg<String>(0)
            val prefsData = prefsDataByName.getOrPut(prefsName) { mutableMapOf() }

            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            val mockPrefs = mockk<SharedPreferences>(relaxed = true)

            every { mockPrefs.getString(any(), any()) } answers {
                prefsData[arg(0)] ?: arg(1)
            }
            every { mockPrefs.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } answers {
                prefsData[arg(0)] = arg(1)
                mockEditor
            }
            every { mockEditor.clear() } answers {
                prefsData.clear()
                mockEditor
            }
            every { mockEditor.apply() } returns Unit

            mockPrefs
        }

        rateLimiter =
            RateLimiter(
                context = context,
                operationType = "test_operation",
                maxRequests = 5,
                windowHours = 1,
            )
        rateLimiter.resetForTesting()
    }

    @After
    fun cleanup() {
        rateLimiter.resetForTesting()
    }

    @Test
    fun `first request should not exceed limit`() {
        rateLimiter.checkLimit()
        rateLimiter.recordRequest()

        assertEquals(4, rateLimiter.getRemainingRequests())
    }

    @Test
    fun `allows requests up to max limit`() {
        repeat(5) {
            rateLimiter.checkLimit()
            rateLimiter.recordRequest()
        }

        assertEquals(0, rateLimiter.getRemainingRequests())
    }

    @Test
    fun `throws RateLimitException when limit exceeded`() {
        repeat(5) {
            rateLimiter.checkLimit()
            rateLimiter.recordRequest()
        }

        try {
            rateLimiter.checkLimit()
            throw AssertionError("Expected RateLimitException to be thrown")
        } catch (e: RateLimitException) {
            assertEquals("test_operation", e.operationType)
            assertTrue(e.remainingMinutes >= 0)
        }
    }

    @Test
    fun `rate limit exception contains correct remaining time`() {
        repeat(5) {
            rateLimiter.checkLimit()
            rateLimiter.recordRequest()
        }

        try {
            rateLimiter.checkLimit()
            throw AssertionError("Expected RateLimitException to be thrown")
        } catch (e: RateLimitException) {
            assertTrue(e.remainingMinutes <= 60)
        }
    }

    @Test
    fun `getRemainingRequests returns correct count`() {
        assertEquals(5, rateLimiter.getRemainingRequests())

        rateLimiter.recordRequest()
        assertEquals(4, rateLimiter.getRemainingRequests())

        rateLimiter.recordRequest()
        assertEquals(3, rateLimiter.getRemainingRequests())
    }

    @Test
    fun `getResetTimeMillis returns null when no requests made`() {
        val resetTime = rateLimiter.getResetTimeMillis()
        assertNull(resetTime)
    }

    @Test
    fun `getResetTimeMillis returns correct reset time`() {
        val beforeRequest = System.currentTimeMillis()
        rateLimiter.recordRequest()
        val afterRequest = System.currentTimeMillis()

        val resetTime = rateLimiter.getResetTimeMillis()
        assertNotNull(resetTime)

        val expectedMinReset = beforeRequest + TimeUnit.HOURS.toMillis(1)
        val expectedMaxReset = afterRequest + TimeUnit.HOURS.toMillis(1)

        assertTrue(resetTime!! >= expectedMinReset)
        assertTrue(resetTime <= expectedMaxReset)
    }

    @Test
    fun `multiple rate limiters with different operations are independent`() {
        val rateLimiter1 =
            RateLimiter(
                context = context,
                operationType = "operation1",
                maxRequests = 2,
                windowHours = 1,
            )
        val rateLimiter2 =
            RateLimiter(
                context = context,
                operationType = "operation2",
                maxRequests = 2,
                windowHours = 1,
            )

        rateLimiter1.resetForTesting()
        rateLimiter2.resetForTesting()

        rateLimiter1.recordRequest()
        rateLimiter1.recordRequest()

        assertEquals(0, rateLimiter1.getRemainingRequests())
        assertEquals(2, rateLimiter2.getRemainingRequests())

        rateLimiter1.resetForTesting()
        rateLimiter2.resetForTesting()
    }

    @Test
    fun `window sliding behavior - old timestamps are ignored`() {
        val shortWindowLimiter =
            RateLimiter(
                context = context,
                operationType = "short_window_test",
                maxRequests = 2,
                windowHours = 1,
            )
        shortWindowLimiter.resetForTesting()

        shortWindowLimiter.recordRequest()
        assertEquals(1, shortWindowLimiter.getRemainingRequests())

        shortWindowLimiter.recordRequest()
        assertEquals(0, shortWindowLimiter.getRemainingRequests())

        Thread.sleep(100)

        assertEquals(0, shortWindowLimiter.getRemainingRequests())

        shortWindowLimiter.resetForTesting()
    }

    @Test
    fun `resetForTesting clears all timestamps`() {
        repeat(5) {
            rateLimiter.recordRequest()
        }

        assertEquals(0, rateLimiter.getRemainingRequests())

        rateLimiter.resetForTesting()

        assertEquals(5, rateLimiter.getRemainingRequests())
        assertNull(rateLimiter.getResetTimeMillis())
    }

    @Test
    fun `rate limiter with different window sizes`() {
        val shortLimiter =
            RateLimiter(
                context = context,
                operationType = "short_test",
                maxRequests = 3,
                windowHours = 1,
            )
        val longLimiter =
            RateLimiter(
                context = context,
                operationType = "long_test",
                maxRequests = 3,
                windowHours = 24,
            )

        shortLimiter.resetForTesting()
        longLimiter.resetForTesting()

        shortLimiter.recordRequest()
        longLimiter.recordRequest()

        assertEquals(2, shortLimiter.getRemainingRequests())
        assertEquals(2, longLimiter.getRemainingRequests())

        shortLimiter.resetForTesting()
        longLimiter.resetForTesting()
    }

    @Test
    fun `rate limiter persists across instances`() {
        val limiter1 =
            RateLimiter(
                context = context,
                operationType = "persist_test",
                maxRequests = 3,
                windowHours = 1,
            )
        limiter1.resetForTesting()

        limiter1.recordRequest()
        assertEquals(2, limiter1.getRemainingRequests())

        val limiter2 =
            RateLimiter(
                context = context,
                operationType = "persist_test",
                maxRequests = 3,
                windowHours = 1,
            )

        assertEquals(2, limiter2.getRemainingRequests())

        limiter2.resetForTesting()
    }

    @Test
    fun `checkLimit does not modify state`() {
        assertEquals(5, rateLimiter.getRemainingRequests())

        rateLimiter.checkLimit()
        assertEquals(5, rateLimiter.getRemainingRequests())

        rateLimiter.checkLimit()
        assertEquals(5, rateLimiter.getRemainingRequests())
    }

    @Test
    fun `recordRequest without checkLimit updates state correctly`() {
        rateLimiter.recordRequest()
        assertEquals(4, rateLimiter.getRemainingRequests())
    }
}

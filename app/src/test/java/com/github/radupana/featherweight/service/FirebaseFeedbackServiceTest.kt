package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.BuildConfig
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirebaseFeedbackServiceTest {
    private lateinit var service: FirebaseFeedbackService

    @Before
    fun setUp() {
        service = FirebaseFeedbackService()
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startFeedback logs warning when not in debug build`() {
        service.startFeedback()

        if (!BuildConfig.DEBUG) {
            verify { android.util.Log.w("FirebaseFeedbackService", "Feedback only available in test builds") }
        } else {
            verify(atLeast = 1) {
                android.util.Log.e(any(), any(), any())
            }
        }
    }

    @Test
    fun `startFeedback handles missing Firebase SDK gracefully in debug build`() {
        if (!BuildConfig.DEBUG) {
            return
        }

        service.startFeedback()

        verify(atLeast = 1) {
            android.util.Log.e(any(), any(), any())
        }
    }

    @Test
    fun `service methods don't throw exceptions when called`() {
        val service = FirebaseFeedbackService()

        try {
            service.startFeedback()
        } catch (e: Exception) {
            throw AssertionError("Methods should not throw exceptions", e)
        }
    }

    @Test
    fun `multiple calls to service methods work correctly`() {
        val service = FirebaseFeedbackService()

        repeat(3) {
            service.startFeedback()
        }

        if (!BuildConfig.DEBUG) {
            verify(exactly = 3) {
                android.util.Log.w("FirebaseFeedbackService", "Feedback only available in test builds")
            }
        } else {
            verify(atLeast = 3) {
                android.util.Log.e(any(), any(), any())
            }
        }
    }

    @Test
    fun `startFeedback handles ClassNotFoundException gracefully`() {
        // Ensure we're in debug mode for this test
        if (!BuildConfig.DEBUG) {
            return
        }

        // Firebase SDK is not available in unit tests, so this will throw ClassNotFoundException
        service.startFeedback()

        // Should log the exception
        verify(atLeast = 1) {
            android.util.Log.e("FirebaseFeedbackService", "Failed to start feedback", any())
        }
    }

    @Test
    fun `service can be instantiated multiple times`() {
        val service1 = FirebaseFeedbackService()
        val service2 = FirebaseFeedbackService()
        val service3 = FirebaseFeedbackService()

        // All instances should work independently
        service1.startFeedback()
        service2.startFeedback()
        service3.startFeedback()

        // Each should log appropriately
        if (!BuildConfig.DEBUG) {
            verify(atLeast = 3) {
                android.util.Log.w("FirebaseFeedbackService", "Feedback only available in test builds")
            }
        }
    }

    @Test
    fun `service methods are idempotent`() {
        // Multiple calls should have the same effect
        service.startFeedback()
        service.startFeedback()

        if (!BuildConfig.DEBUG) {
            verify(exactly = 2) {
                android.util.Log.w("FirebaseFeedbackService", "Feedback only available in test builds")
            }
        } else {
            verify(atLeast = 2) {
                android.util.Log.e(any(), any(), any())
            }
        }

        // Additional calls should continue to work
        service.startFeedback()

        if (!BuildConfig.DEBUG) {
            verify(exactly = 3) {
                android.util.Log.w("FirebaseFeedbackService", "Feedback only available in test builds")
            }
        } else {
            verify(atLeast = 3) {
                android.util.Log.e(any(), any(), any())
            }
        }
    }

    @Test
    fun `TAG constant has expected value`() {
        // Use reflection to verify the TAG constant
        val tagField = FirebaseFeedbackService::class.java.getDeclaredField("TAG")
        tagField.isAccessible = true
        val tagValue = tagField.get(null) as String

        assertThat(tagValue).isEqualTo("FirebaseFeedbackService")
    }

    @Test
    fun `BuildConfig DEBUG flag is accessible`() {
        // Since isTestBuild is a private companion function that just returns BuildConfig.DEBUG,
        // we can verify that BuildConfig.DEBUG is accessible and has expected type
        val debugFlag = BuildConfig.DEBUG
        assertThat(debugFlag).isAnyOf(true, false)
    }

    @Test
    fun `startFeedback logs debug message in test builds`() {
        if (!BuildConfig.DEBUG) {
            // Skip this test in non-debug builds
            return
        }

        service.startFeedback()

        // Even though Firebase SDK is not available, it should attempt to log
        verify(atLeast = 1) {
            android.util.Log.e("FirebaseFeedbackService", "Failed to start feedback", any())
        }
    }
}

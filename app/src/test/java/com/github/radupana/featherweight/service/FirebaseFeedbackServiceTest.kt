package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.BuildConfig
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
}

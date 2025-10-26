package com.github.radupana.featherweight.testutil

import io.mockk.every
import io.mockk.mockkStatic

/**
 * Mock Android Log class for unit tests
 */
object LogMock {
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any()) } returns 0
        every { android.util.Log.v(any<String>(), any<String>()) } returns 0
        every { android.util.Log.isLoggable(any<String>(), any<Int>()) } returns false
    }
}

package com.github.radupana.featherweight.util

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExceptionLoggerTest {
    @Before
    fun setUp() {
        mockkObject(CloudLogger)
        every { CloudLogger.debug(any(), any(), any<Throwable>()) } returns Unit
        every { CloudLogger.warn(any(), any(), any<Throwable>()) } returns Unit
        every { CloudLogger.error(any(), any(), any<Throwable>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(CloudLogger)
    }

    @Test
    fun `logException logs error level by default`() {
        val exception = RuntimeException("Test error")

        ExceptionLogger.logException("TestTag", "Error occurred", exception)

        verify {
            CloudLogger.error("TestTag", "Error occurred: Test error", exception)
        }
    }

    @Test
    fun `logException logs with debug level when specified`() {
        val exception = IllegalArgumentException("Invalid argument")

        ExceptionLogger.logException(
            "TestTag",
            "Debug message",
            exception,
            ExceptionLogger.LogLevel.DEBUG,
        )

        verify {
            CloudLogger.debug("TestTag", "Debug message: Invalid argument", exception)
        }
    }

    @Test
    fun `logException logs with warning level when specified`() {
        val exception = IllegalStateException("Bad state")

        ExceptionLogger.logException(
            "TestTag",
            "Warning message",
            exception,
            ExceptionLogger.LogLevel.WARNING,
        )

        verify {
            CloudLogger.warn("TestTag", "Warning message: Bad state", exception)
        }
    }

    @Test
    fun `logNonCritical logs with debug level`() {
        val exception = RuntimeException("Non-critical error")

        ExceptionLogger.logNonCritical("TestTag", "Minor issue", exception)

        verify {
            CloudLogger.debug("TestTag", "Minor issue: Non-critical error", exception)
        }
    }

    @Test
    fun `logException handles null exception message`() {
        val exception = RuntimeException()

        ExceptionLogger.logException("TestTag", "Error with null message", exception)

        verify {
            CloudLogger.error("TestTag", "Error with null message: null", exception)
        }
    }
}

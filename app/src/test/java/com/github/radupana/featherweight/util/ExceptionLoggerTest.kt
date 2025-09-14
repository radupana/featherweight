package com.github.radupana.featherweight.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ExceptionLoggerTest {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @Before
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @After
    fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun `logException logs error level by default`() {
        val exception = RuntimeException("Test error")

        ExceptionLogger.logException("TestTag", "Error occurred", exception)

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("[TestTag] Error occurred: Test error")
        assertThat(output).contains("RuntimeException")
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

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("[TestTag] Debug message: Invalid argument")
        assertThat(output).contains("IllegalArgumentException")
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

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("[TestTag] Warning message: Bad state")
        assertThat(output).contains("IllegalStateException")
    }

    @Test
    fun `logNonCritical logs with debug level`() {
        val exception = RuntimeException("Non-critical error")

        ExceptionLogger.logNonCritical("TestTag", "Minor issue", exception)

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("[TestTag] Minor issue: Non-critical error")
        assertThat(output).contains("RuntimeException")
    }

    @Test
    fun `logException handles null exception message`() {
        val exception = RuntimeException()

        ExceptionLogger.logException("TestTag", "Error with null message", exception)

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("[TestTag] Error with null message: null")
    }

    @Test
    fun `logException prints stack trace`() {
        val exception = RuntimeException("Test error")

        ExceptionLogger.logException("TestTag", "Error", exception)

        val output = outputStreamCaptor.toString()
        assertThat(output).contains("at com.github.radupana.featherweight.util.ExceptionLoggerTest")
    }
}
